package com.elyxor.xeros.ldcs.dai;

import com.elyxor.xeros.ldcs.HttpFileUploader;
import com.elyxor.xeros.ldcs.util.FileLogWriter;
import com.elyxor.xeros.ldcs.util.LogWriterInterface;
import com.elyxor.xeros.ldcs.util.SerialReader;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class DaiPort implements DaiPortInterface {

	final static Logger logger = LoggerFactory.getLogger(DaiPort.class);

	private SerialPort serialPort;
	private int daiNum;
	private String daiPrefix;


    private LogWriterInterface _logWriter;

    private Path logFilePath;
    private LogWriterInterface waterMeterLogWriter;
    private long[] prevMeters = new long[2];

    private long meterDiff1;
    private long meterDiff2;

    private boolean logSent;
    boolean meterClearProcessed = false;


    public DaiPort (SerialPort port, int num, LogWriterInterface logWriter, String prefix) {
		this.serialPort = port;
		this.daiNum = num;
		this._logWriter = logWriter;
		this.daiPrefix = prefix;
	}

	public boolean openPort() {
		boolean result = false;
		try {
			result = this.serialPort.openPort();
			this.serialPort.setParams(4800, 7, 1, 2, false, false); //specific serial port parameters for Xeros DAQ
			this.serialPort.addEventListener(new SerialReader(this));
			Thread.sleep(5000); //init time
	    	logger.info("Started listening on port " + this.serialPort.getPortName() +" "+ result);
		} catch (Exception ex) {
			logger.warn("Could not open port", ex);
			result = false;
		}
		return result;
	}
	public String clearPortBuffer() {
		String result = "";
		try {
			result = this.serialPort.readString();
		}
		catch (Exception e) {
			String msg = "Failed to clear port buffer: ";
			logger.warn(msg , e);
			result = msg + e.getMessage();
		}
		return result;
	}
	public String getRemoteDaiId() {
        return parseDaqId(this.getConfig());
//		String result = "";
//		try {
//			this.serialPort.removeEventListener();
//			this.serialPort.writeString("0\n");
//            Thread.sleep(50);
//            logger.info(this.serialPort.readString());
//            this.serialPort.writeString("19\n");
//			Thread.sleep(1000);
//			result = this.serialPort.readString();
//			Thread.sleep(4000); //let the DAQ timeout to avoid setting a new id accidentally
//			this.serialPort.addEventListener(new SerialReader(this));
//            logger.info("read dai id: "+result);
//		}
//		catch (Exception e) {
//			logger.warn("Couldn't read dai id", e);
//		}
//		return result;
	}
	public String setRemoteDaiId(int id) {
		String result = "";
		try {
			this.serialPort.removeEventListener();
            this.serialPort.writeString("0\n");
            Thread.sleep(50);
            logger.info(this.serialPort.readString());
            this.serialPort.writeString("19\n");
			Thread.sleep(1000);
			this.serialPort.readString(); //clear buffer, DAQ writes old daiId
            logger.info("setting new id: " + id);
			this.serialPort.writeString(id+"\n");
			Thread.sleep(1000);
			result = this.serialPort.readString();
			this.serialPort.addEventListener(new SerialReader(this));
		}
		catch (Exception e) {
			logger.warn("Couldn't set port id", e);
		}
		this.setDaiNum(id);
		return result;
	}
	public String sendStdRequest() {
		String result = "";
		try {
			this.serialPort.removeEventListener();
            this.serialPort.writeString("0\n");
            Thread.sleep(50);
            logger.info(this.serialPort.readString());
            this.serialPort.writeString("12\n");
			Thread.sleep(1000);
			while (this.serialPort.getInputBufferBytesCount() > 0) {
				result += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
				Thread.sleep(500);
			}
			this.serialPort.addEventListener(new SerialReader(this));

		} catch (Exception e) {
			String msg = "Couldn't complete send std request. ";
			logger.warn(msg, e);
			result = msg + e.getMessage(); 
		}
		return result;
	}
	public String sendXerosRequest() {
		String result = "";
		try {
			this.serialPort.removeEventListener();
            this.serialPort.writeString("0\n");
            Thread.sleep(50);
            logger.info(this.serialPort.readString());
            this.serialPort.writeString("11\n");
			Thread.sleep(1000);
			while (this.serialPort.getInputBufferBytesCount() > 0) {
				result += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
				Thread.sleep(500);
			}
			this.serialPort.addEventListener(new SerialReader(this));
		} catch (Exception e) {
			String msg = ("Couldn't complete send xeros request. ");
			logger.warn(msg, e);
			result = msg + e.getMessage(); 
		}
		return result;
	}

	public String initWaterRequest() {
        this.setLogFilePath(Paths.get(this._logWriter.getPath().getParent().toString(), "/waterMeters"));
        this.setWaterMeterLogWriter(new FileLogWriter(this.logFilePath, daiPrefix +this.getDaiNum()+ "meterLogging.txt"));

        long[] meters = parsePrevMetersFromFile();
        logSent = true;
        if (Arrays.equals(meters, new long[]{0, 0})) {
            logSent = false;
            try {
                String buffer = this.sendWaterRequest();
                meters = this.parseMetersFromResponse(buffer);
            } catch (Exception e) {
                logger.warn("unable to complete water request for parsing", e);
            }
            this.storePrevMeters(meters);
        }
        return Arrays.toString(meters);
    }

    private long[] parseMetersFromResponse(String response) {
        long[] result = new long[2];
        if (response == null || response.equals("")) return result;
        logger.info("response: " + response);
        String[] lineData = response.split("\n");
        logger.info("lineData Length: "+lineData.length);
        for (String line : lineData) {
            logger.info("line" + line);

            //Cold water, WM 0 for Xeros and WM 2 for Non-Xeros
            if (line.startsWith("WM 0") || line.startsWith("WM 2")) {
                String[] lineSplit = line.split(",");
                logger.info("lineSplit Length:" + lineSplit.length);
                logger.info("lineSplit[3]: " + lineSplit[3]);
                result[0] = Long.parseLong(lineSplit[3].trim());
            }

            //Hot water, WM1 for Xeros and WM 3 for Non-Xeros
            if (line.startsWith("WM 1") || line.startsWith("WM 3")) {
                String[] lineSplit = line.split(",");
                logger.info("lineSplit Length:" + lineSplit.length);
                logger.info("lineSplit[3]: " + lineSplit[3]);
                result[1] = Long.parseLong(lineSplit[3].trim());
            }
        }
        return result;
    }

	public String sendWaterRequest() throws Exception {
        String result = "";
        int retry = 0;
        this.serialPort.removeEventListener();
        while (retry < 3) {
            this.serialPort.writeString("0\n");
            Thread.sleep(50);
            logger.info(this.serialPort.readString());
            this.serialPort.writeString("13\n");
            Thread.sleep(1000);
            while (this.serialPort.getInputBufferBytesCount() > 0) {
                result += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
                Thread.sleep(500);
            }
            if (result!=null && result.length() > 40) break;
            result = "";
            Thread.sleep(4000);
            retry++;
        }
        logger.info("water request", "retry: " + retry + " result: " + result);
        this.serialPort.addEventListener(new SerialReader(this));
		return result;
	}

    public String sendXerosWaterRequest() throws Exception {
        String result = "";
        int retry = 0;
        this.serialPort.removeEventListener();
        while (retry < 3) {
            this.serialPort.writeString("0\n");
            Thread.sleep(50);
            logger.info(this.serialPort.readString());
            this.serialPort.writeString("11\n");
            Thread.sleep(1000);
            while (this.serialPort.getInputBufferBytesCount() > 0) {
                result += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
                Thread.sleep(500);
            }
            if (result!=null && result.length() > 40) break;
            result = "";
            retry++;
        }
        this.serialPort.addEventListener(new SerialReader(this));
        return result;
    }

    public long[] calculateWaterLog(String buffer) {
        long[] result = null;
        long[] prevMeters = this.parsePrevMetersFromFile();
        long[] currentMeters = this.parseMetersFromResponse(buffer);
        if (currentMeters[0] == 0 && currentMeters[1] == 0) {
            logger.warn("no water meter readings found");
            return result;
        }
        logger.info("Captured log file, meter1: "+currentMeters[0]+", meter2: "+currentMeters[1]);

        long meter1 = currentMeters[0] - prevMeters[0];
        long meter2 = currentMeters[1] - prevMeters[1];

        logger.info("Water Diff: meter1: "+meter1+", meter2: "+meter2);

        if (meter1 == 0 && meter2 == 0) {
            if (!logSent) {
                result = new long[2];
                result[0] = meterDiff1;
                result[1] = meterDiff2;
                meterDiff1 = meterDiff2 = 0;
                logSent = true;

                currentMeters = new long[]{0, 0};

                clearWaterMeters();

                return result;
            }
            return result;
        }
        meterDiff1 += meter1;
        meterDiff2 += meter2;
        try {
            Files.delete(this.getWaterMeterLogWriter().getFile().toPath());
        } catch (Exception e) {
            String msg = "water meter log file not found";
            logger.warn(msg,e);
        }
        logSent = false;
        this.storePrevMeters(currentMeters);
        return result;
    }

    private void clearWaterMeters() {
        try {
            this.serialPort.writeString("0\n");
            Thread.sleep(50);
            logger.info(this.serialPort.readString());
            serialPort.writeString("113\n");
            Thread.sleep(500);
            this.serialPort.writeString("0\n");
            Thread.sleep(50);
            logger.info(this.serialPort.readString());
            serialPort.writeString("113\n");
            Thread.sleep(500);
            this.serialPort.writeString("0\n");
            Thread.sleep(50);
            logger.info(this.serialPort.readString());
            serialPort.writeString("113\n");
            Thread.sleep(500);
            logger.info("cleared water logs");
        } catch (Exception e) {
            String msg = "Failed to clear water meters";
            logger.warn(msg, e);
        }
    }

    public void writeWaterOnlyLog(long[] meters) {
        try {
            this._logWriter.write(this.daiPrefix+this.getDaiNum() + ", Std , \nFile Write Time: , "
            + getSystemTimeAndDate() + "\n"
            + "WM2: , 0 , 0 , "
            + meters[0] + "\n"
            + "WM3: , 0 , 0 , "
            + meters[1]);
        } catch (IOException e) {
            logger.warn("failed to write " + Arrays.toString(meters) + "to log file.");
        }
        logger.info("wrote water meter log to file");
    }

    @Override
    public void writeWaterOnlyXerosLog(long[] meters) {
        try {
            this._logWriter.write(this.daiPrefix+this.getDaiNum() + ", Xeros , \nFile Write Time: , "
                    + getSystemTimeAndDate() + "\n"
                    + "WM2: , 0 , 0 , "
                    + meters[0] + "\n"
                    + "WM3: , 0 , 0 , "
                    + meters[1]);
        } catch (IOException e) {
            logger.warn("failed to write " + Arrays.toString(meters) + "to log file.");
        }
        logger.info("wrote water meter log to file");

    }

    public String sendRequest() {
		String result = "";	    		
		try {
            this.serialPort.writeString("0\n");
            Thread.sleep(50);
            logger.info(this.serialPort.readString());
            this.serialPort.writeString("999\n");
			Thread.sleep(1000);
			while (this.serialPort.getInputBufferBytesCount() > 0) {
				result += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
				Thread.sleep(500);
			}
		} catch (Exception e) {
			logger.warn("Couldn't complete send request", e);
		}
		if (!result.equals("")) logger.info("Captured log file");
		return result;
	}	
		
	public String setClock() {
        String result = "";
        try {
            this.serialPort.removeEventListener();
            this.serialPort.writeString("0\n");
            Thread.sleep(50);
            logger.info(this.serialPort.readString());
            this.serialPort.writeString("16\n");
            String[] timeSplit = this.getSystemTime().replaceAll(" ","").split(":");
            Thread.sleep(100);
            this.serialPort.writeString(timeSplit[0] + "\n");
            Thread.sleep(100);
            this.serialPort.writeString(timeSplit[1] + "\n");
            Thread.sleep(100);
            this.serialPort.writeString(timeSplit[2] + "\n");
            Thread.sleep(500);
            result = this.serialPort.readString();
            this.serialPort.addEventListener(new SerialReader(this));
        } catch (Exception e) {
            result = "Couldn't complete set clock. ";
            logger.warn(result, e);
            result = result + e.getMessage();
        }
        if (result != "") logger.info("Successfully set clock to: "+result);
        return result;
    }

	public String readClock() {
		String result = "";
		try {
			this.serialPort.removeEventListener();
            this.serialPort.writeString("0\n");
            Thread.sleep(50);
            logger.info(this.serialPort.readString());
            this.serialPort.writeString("15\n");
			Thread.sleep(5000);
			result = this.serialPort.readString();
			
			this.serialPort.addEventListener(new SerialReader(this));
		} catch (Exception e) {
			result = "Couldn't complete read clock. ";
			logger.warn(result, e);
			result = result + e.getMessage();
		}
		return result;
	}

    public String getConfig() {
        String result = "";
        int retryCounter = 0;
        try {
            while (retryCounter < 3) {
                this.serialPort.removeEventListener();
                this.serialPort.writeString("0\n");
                Thread.sleep(50);
                logger.info(this.serialPort.readString());
                this.serialPort.writeString("10\n");
                Thread.sleep(500);
                while (this.serialPort.getInputBufferBytesCount() > 0) {
                    result += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
                    Thread.sleep(500);
                }
                this.serialPort.addEventListener(new SerialReader(this));
                if (!result.equals("")) {
                    logger.info("Captured config");
                    break;
                }
                retryCounter++;
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            logger.warn("Couldn't complete config request", e);
        }
        return result;
    }
	
	public boolean closePort() {
		String portAddress = this.serialPort.getPortName();
		boolean result = false;
		try {
			this.serialPort.removeEventListener();
			result = this.serialPort.closePort();
		}
		catch (SerialPortException ex) {
			logger.warn("Failed to close port "+portAddress);
			result = false;
		}
        logger.info("Closed port: "+result);
		return result;
	}
	public void writeLogFile(String buffer) {
		if (buffer != null) {
            String editedBuffer = this.addDateStamp(buffer);
			String[] bufferSplit = editedBuffer.split(",");
			String logPrefix = "";
			if (1 < bufferSplit.length) logPrefix = bufferSplit[1].trim();
			
			LogWriterInterface writer = new FileLogWriter(this._logWriter.getPath().getParent(), logPrefix+"-"+this._logWriter.getFilename());
			try {
				writer.write(this.daiPrefix + editedBuffer);
			} catch (IOException e) {
				logger.warn("Failed to write '" + buffer + "' to log file", e);
			}
			logger.info("successfully sent log to filewriter");
		}
    }
	public boolean ping() {
		int responseStatus = new HttpFileUploader().postPing(daiPrefix+this.getDaiNum());
		if (responseStatus == 200) {
			logger.info("successfully pinged server");
			return true;
		}
		else {
			logger.info("failed to ping server due to http response");
			return false;
		}
	}
    public boolean sendMachineStatus() {
        byte[] statusBytes = parseActiveStates(this.getConfig());
        int responseStatus = new HttpFileUploader().postMachineStatus(daiPrefix+this.getDaiNum(), statusBytes);
        if (responseStatus == 200) {
            logger.info("successfully sent machine status");
            return true;
        }
        logger.info("failed to send machine status due to http response:" + responseStatus);
        return false;
    }

    private String getSystemTimeAndDate() {
        String result = "";
        SimpleDateFormat timingFormat = new SimpleDateFormat("dd-MM-yyyy kk : mm : ss");
        result = timingFormat.format(System.currentTimeMillis());
        return result;
    }

    private String getSystemTime() {
        String result = "";
        SimpleDateFormat timingFormat = new SimpleDateFormat("kk : mm : ss");
        result = timingFormat.format(System.currentTimeMillis());
        return result;
    }

    private long[] parsePrevMetersFromFile() {
        byte[] inputData = null;
        try {
            inputData = IOUtils.toByteArray(new FileReader(this.getWaterMeterLogWriter().getFile()));
        } catch (Exception ex) {
            logger.warn("could not open meter log file",ex);
            return new long[]{0,0};
        }

        StringBuffer fString = new StringBuffer();
        for ( byte b : inputData ){
            if( (int)b<10 ) {
                continue;
            }
            fString.append((char)b);
        }
        String[] lines = fString.toString().split("\n");
        long[] prev = new long[lines.length];
        for (int i = 0; i < lines.length; i++) {
            if (lines[i]!=null) {
                String[] lineData = lines[i].split(",");
                prev[i] = Long.parseLong(lineData[1]);
            }
        }
        return prev;
    }
    private void storePrevMeters(long[] meters) {
        for (int i = 0; i < meters.length; i++) {
            try {
                this.getWaterMeterLogWriter().write("meter"+i+","+meters[i] +","+ getSystemTimeAndDate()+"\n");
                logger.info("successfully stored previous meter "+i+" in log file");
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("failed to store previous meters",e);
            }
        }
    }

    public void setPrevMeters(long meter1, long meter2) {
        prevMeters[0] = meter1;
        prevMeters[1] = meter2;
    }
    public LogWriterInterface getWaterMeterLogWriter() {
        return waterMeterLogWriter;
    }
    public void setWaterMeterLogWriter(LogWriterInterface waterMeterLogWriter) {
        this.waterMeterLogWriter = waterMeterLogWriter;
    }
    public Path getLogFilePath() {
        return logFilePath;
    }
    public void setLogFilePath(Path logFilePath) {
        this.logFilePath = logFilePath;
    }

    public LogWriterInterface getLogWriter() {
        return _logWriter;
    }

    public void setLogWriter(LogWriterInterface logWriter) {
        this._logWriter = logWriter;
    }

    public SerialPort getSerialPort() {
		return serialPort;
	}
	public void setSerialPort(SerialPort port) {
		this.serialPort = port;
	}
	public int getDaiNum() {
		return daiNum;
	}
	public void setDaiNum(int id) {
		this.daiNum = id;
	}

    private String addDateStamp(String buffer) {
        String[] split = buffer.split(": ,");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String date = dateFormat.format(System.currentTimeMillis());
        return split[0] +": , "+ date + split[1];
    }

    private byte[] parseActiveStates(String buffer){
        byte[] activeStates = {-1,-1};
        if (buffer==null || buffer.equals(""))
            return activeStates;

        String[] lines = buffer.split("\n");
        for (String line : lines) {
            if (line.startsWith("\rDigEvent")) {
                String[] lineData = line.split(" ");
                activeStates[0] = Byte.valueOf(lineData[1]);
                activeStates[1] = Byte.valueOf(lineData[9]);
            }
        }
        return activeStates;
    }

    private String parseDaqId(String buffer) {
        String daqId = "-1";
        if (buffer == null || buffer.equals(""))
            return daqId;

        String[] lines = buffer.split("\n");
        for (String line : lines) {
            if (line.contains("DAI")) {
                String[] lineData = line.split(" ");
                daqId = lineData[3];
            }
        }
        return daqId;
    }
}
