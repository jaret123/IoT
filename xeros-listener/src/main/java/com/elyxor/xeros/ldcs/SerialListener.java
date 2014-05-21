package com.elyxor.xeros.ldcs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialListener {
	
	final static Logger logger = LoggerFactory.getLogger(SerialListener.class);
	
	static Map<String,DaiPort> portList = new LinkedHashMap<String,DaiPort>();
	public static String daiPrefix = AppConfiguration.getDaiName()+"00";
	public static int nextDaiNum;
	public static String daiName = "";
	
	private static final String EOT = new String(new char[] {'\004','\012'}); 
	
	static volatile boolean portFinderRunning;

//	public static void main(String[] args) {
//		(new Thread(new commandListener())).start();
//		(new Thread(new PortFinder())).start();
//	}
//	
	
    static class commandListener implements Runnable {
    	public void run() {
    		logger.info("Listening to command line inputs");
    		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    		String line = "";
    		
			Map<Integer, DaiPort> activeDaiPorts = new HashMap<Integer, DaiPort>();
			
			while (line.equalsIgnoreCase("quit") == false) {

				try {
					line = in.readLine();
				} catch (IOException e) {logger.warn("Failed to start command line listener", e);}
    			
    			if (line.equalsIgnoreCase("list")) {
					int i = 1;
    				for (DaiPort port : portList.values()) {
    					activeDaiPorts.put(i, port);
    					System.out.println(i + " - " + daiPrefix + port.getDaiNum());
    					i++;
    				}
       			}
    			
    			if (line.equalsIgnoreCase("stop")) {
    				shutdownListener();
    			}
    			
    			int portnum = 0;
    			int bufferint = 0;
    			String buffer = "";
    			int daiId = 0;
    			
    			if (line.equalsIgnoreCase("getid")) {    				
    				System.out.println("enter port number");
    				
    				try {
						portnum = Integer.parseInt(in.readLine());
						DaiPort daiPort = activeDaiPorts.get(portnum);
						bufferint = daiPort.getRemoteDaiId();		
					} catch (Exception e) {logger.warn("Failed to complete get dai id operation", e);}
    				System.out.println(bufferint);
    			}
    			
    			if (line.equalsIgnoreCase("setid")) {
    				System.out.println("enter port number");
    				
    				try {
						portnum = Integer.parseInt(in.readLine());
						System.out.println("enter new dai id");
    				
						daiId = Integer.parseInt(in.readLine());
						
						DaiPort daiPort = activeDaiPorts.get(portnum);
						buffer = daiPort.setRemoteDaiId(daiId);
						
					} catch (Exception e) {logger.warn("Failed to complete operation set dai id", e);}
    				System.out.println(buffer);
    			}
    			
    			if (line.equalsIgnoreCase("sendstd")) {
    				System.out.println("enter port number");
    				
    				try {
    					portnum = Integer.parseInt(in.readLine());
    					
    					DaiPort daiPort = activeDaiPorts.get(portnum);
    					buffer = daiPort.sendStdRequest();
    					daiPort.writeLogFile(buffer);
    				} catch (Exception ex) {logger.warn("failed to send std request", ex);}
    				System.out.println(buffer);
    			}
    			
    			if (line.equalsIgnoreCase("sendx")) {
    				System.out.println("enter port number");
    				
    				try {
    					portnum = Integer.parseInt(in.readLine());
    					
    					DaiPort daiPort = activeDaiPorts.get(portnum);
    					buffer = daiPort.sendXerosRequest();
    					daiPort.writeLogFile(buffer);
    					
    				} catch (Exception ex) {logger.warn("failed to send xeros request", ex);}
    				System.out.println(buffer);
    			}

    			if (line.equalsIgnoreCase("clear")) {
    				System.out.println("enter port number");
    				try {
    					portnum = Integer.parseInt(in.readLine());
    					
    					DaiPort daiPort = activeDaiPorts.get(portnum);
    					buffer = daiPort.getSerialPort().readString();
    				} catch (Exception ex) {logger.warn("failed to clear port buffer", ex);}
    				System.out.println(buffer);
    			}
    			
    			if (line.equalsIgnoreCase("setclock")) {
    				System.out.println("enter port number");
    				try {
    					portnum = Integer.parseInt(in.readLine());
    					
    					DaiPort daiPort = activeDaiPorts.get(portnum);
    					daiPort.setClock();
    					buffer = daiPort.getSerialPort().readString();
    				} catch (Exception ex) {logger.warn("failed to set clock", ex);}
    				System.out.println(buffer);
    			}
    			if (line.equalsIgnoreCase("readclock")) {
    				System.out.println("enter port number");
    				try {
    					portnum = Integer.parseInt(in.readLine());
    					DaiPort daiPort = activeDaiPorts.get(portnum);
    					buffer = daiPort.readClock();
    				} catch (Exception ex) {logger.warn("failed to read clock", ex);}
    				System.out.println(buffer);
    			}
    		}
    	}
    }
	
	static class PortFinder implements Runnable {
		
		public void run() {
			logger.info("Watching for new serial devices");
			
			nextDaiNum = 1;
			portFinderRunning = true;
			
			while (portFinderRunning) {
				daiName = daiPrefix + nextDaiNum;

//				List<String> localPorts = Arrays.asList("/dev/pts/29", "/dev/pts/30");
				
				List<String> localPorts = Arrays.asList(SerialPortList.getPortNames());				
				List<String> activePortAddresses = new ArrayList<String>(portList.keySet());
				
				if (!portList.isEmpty()) {						
					for (String portAddress : activePortAddresses) {	
						if (!localPorts.contains(portAddress)) {
							DaiPort activePort = portList.get(portAddress);
							activePort.closeDaiPort();
							nextDaiNum = activePort.getDaiNum();
						}
					}
				}
				for (String portAddress : localPorts) {
					if (!portList.containsKey(portAddress)) {				
						DaiPort port = new DaiPort(new SerialPort(portAddress), nextDaiNum);
						port.openDaiPort();
						nextDaiNum++;
						break;
					}
				} 
			}
			if (Thread.interrupted()) {
				logger.info("Ending watch for new serial devices ");
				return;
			}
		}
	}
	
	static class SerialReader implements SerialPortEventListener {
    	SerialPort serialPort;
    	DaiPort daiPort;
    	
    	public SerialReader(DaiPort port) {
	    	daiPort = port;
    		serialPort = port.getSerialPort();
    	} 
    	
		public void serialEvent(SerialPortEvent event) {
			String eventBuffer = "";
			String logBuffer ;
			
			if (event.isRXCHAR()) {
				if (event.getEventValue() == 3) {
					try {
						eventBuffer = serialPort.readString(3);
					} catch (Exception ex) {
						logger.warn("Failed to read serial event", ex);
					}
					if (!eventBuffer.isEmpty() && eventBuffer.equals("***")) {
						logBuffer = daiPort.sendRequest();
						if (!logBuffer.isEmpty() && logBuffer.endsWith(EOT)) {
							daiPort.writeLogFile(logBuffer);
						}
					}
				}
	    	}
		}	
	}
    
    static class DaiPort {
    	private SerialPort serialPort;
    	private int daiNum;

    	public DaiPort (SerialPort port, int num) { 
    		this.serialPort = port;
    		this.daiNum = num;
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
    	
    	public boolean openDaiPort() {
    		SerialPort port = this.serialPort;
    		String portAddress = port.getPortName();
    		
    		boolean success = false;
    		int daiId = 0;
    		
    		try {
    			success = port.openPort();
    			port.setParams(4800, 7, 1, 2, false, false);
    			
				daiId = this.getRemoteDaiId();
											
				if (daiId == 0 || !(daiId == nextDaiNum)) {
					this.setRemoteDaiId(nextDaiNum);
					portList.put(portAddress, this);
			    	logger.info("Assigned DAI ID "+daiName+nextDaiNum+" to port "+portAddress);	
				}
				
				port.addEventListener(new SerialReader(this)); //TODO:switch to test version
				
		    	logger.info("Started listening on port " + port.getPortName());
	
    		} catch (Exception ex) {
    			logger.warn("Could not open port", ex);
    		}
    		return success;
    	}
    	
    	public int getRemoteDaiId() {
    		int daiId = 0;
    		try {
    			this.serialPort.writeString("13\n");
    			daiId = this.serialPort.readBytes(1)[0];
    		}
    		catch (Exception e) {
    			logger.warn("Couldn't read dai id", e);
    		}
    		return daiId;
    	}
    	
    	public String setRemoteDaiId(int id) {
    		String daiId = "";
    		try {
    			this.serialPort.writeString("0 13\n");
				Thread.sleep(2000);
				daiId = this.serialPort.readString();
    			this.serialPort.writeString(id+"\n");
				Thread.sleep(5000);
				daiId += this.serialPort.readString();
    		}
    		catch (Exception e) {
    			logger.warn("Couldn't set port id", e);
    		}
			this.setDaiNum(id);
			return daiId;
    	}
    	
    	public String sendStdRequest() {
			String buffer = "";	    		
			try {
				this.serialPort.writeString("0 12\n");
				Thread.sleep(10000);
				buffer = this.serialPort.readString();
			} catch (Exception e) {
				logger.warn("Couldn't complete send std request", e);
			}
    		return buffer;
    	}
    	
    	public String sendXerosRequest() {
			String buffer = "";	    		
			try {
				this.serialPort.writeString("0 11\n");
				Thread.sleep(10000);
				buffer = this.serialPort.readString();
			} catch (Exception e) {
				logger.warn("Couldn't complete send xeros request", e);
			}
    		return buffer;
    	}
    	
    	public String sendRequest() {
			String buffer = "";	    		
			try {
				this.serialPort.writeString("0 999\n");
				Thread.sleep(10000);
				buffer = this.serialPort.readString();
			} catch (Exception e) {
				logger.warn("Couldn't complete send request", e);
			}
    		return buffer;
    	}
    	
    	public String setClock() {
			String buffer = "";
    		try {
    			this.serialPort.writeString("55\n");
    			Thread.sleep(5000);
    			
    			SimpleDateFormat timingFormat = new SimpleDateFormat("hh:mm:ss");
    			buffer = timingFormat.format(System.currentTimeMillis());
    			String[] timeSplit = buffer.split(":");
    			
    			this.serialPort.writeString(timeSplit[0]+"\n");
    			this.serialPort.writeString(timeSplit[1]+"\n");
    			this.serialPort.writeString(timeSplit[2]+"\n");
				Thread.sleep(5000);
				buffer = this.serialPort.readString();
    		} catch (Exception e) {
    			logger.warn("Couldn't complete set time", e);
    		}
    		return buffer;
    	}
    	
    	public String readClock() {
    		String buffer = "";
    		try {
    			this.serialPort.writeString("44\n");
    			Thread.sleep(5000);
    			buffer = this.serialPort.readString();
    		} catch (Exception ex) {logger.warn("Couldn't complete read clock", ex);}
    		return buffer;
    	}
    	
    	
    	public boolean closeDaiPort() {
    		SerialPort port = this.serialPort;
    		String portAddress = port.getPortName();
    		try {
    			port.removeEventListener();
    			port.closePort();
    			portList.remove(portAddress);
    		}
    		catch (SerialPortException ex) {
    			logger.warn("Failed to remove port "+portAddress);
    			return false;
    		}
    		logger.info("Removed port "+portAddress);
    		return true;
    	}
        
    	public void writeLogFile(String buffer) {
//    		Path dir = Paths.get("/home/will/ldcs");
        	Path dir = Paths.get(AppConfiguration.getLocalPath());
        	        	
        	String logName = daiPrefix+daiNum+"log.txt";
        	
    		File file = new File(dir.toString(), logName);
   			try {
				FileWriter fileWriter = new FileWriter(file, true);
				BufferedWriter out = new BufferedWriter(fileWriter);
				
				out.write(daiPrefix + buffer);
				
				out.close();
				fileWriter.close();
				
			} catch (IOException e) {
				logger.warn("Failed to write log file", e);
			}
        }
    }
        
    public static void shutdownListener() {
		portFinderRunning = false;
		logger.info("Stopped listening for new ports");
		for (Entry<String,DaiPort> entry : portList.entrySet()) {
			DaiPort port = entry.getValue();
			port.closeDaiPort();
		}
	}
    
    
    static class SerialReaderTest implements SerialPortEventListener {
    	SerialPort serialPort;
    	DaiPort daiPort;
    	
    	public SerialReaderTest(DaiPort port) {
    		daiPort = port;
    		serialPort = port.getSerialPort();
    	} 
    	
		public void serialEvent(SerialPortEvent event) {
			File file = null;
			Path dir;
			String buffer;
			int testNum = 1;
						
			if (event.isRXCHAR()) {
				if (event.getEventValue() == 2) {
					try {
						buffer = serialPort.readString();
						
						if (buffer.equals("11")) {
							serialPort.writeString(990+testNum+"\n");
						}
						else if (buffer.equals("12")) {
							daiPort.sendRequest();
						}
					} catch (Exception ex) {
						logger.warn("Failed to start data stream", ex);
					}
				}
				if (event.getEventValue() > 2) {
					try {
						buffer = serialPort.readString(event.getEventValue());

						if (buffer.endsWith("^M^M^M")) {
							dir = Paths.get(AppConfiguration.getLocalPath());
							file = new File(dir.toString(), "output.txt");
							FileWriter writer = new FileWriter(file, true);
							writer.write(daiName + buffer);
							writer.close();
							
							testNum++;
						}
					} catch (SerialPortException ex) {
						logger.warn("Failed to read data stream from "+serialPort.getPortName(), ex);
					} catch (IOException ex) {
						logger.warn("Failed to write log file to "+file.getName(), ex);
					}
				}				
			}
		}
    }
}


