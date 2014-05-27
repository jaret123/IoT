package com.elyxor.xeros.ldcs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
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

import com.elyxor.xeros.ldcs.dai.DaiPort;
import com.elyxor.xeros.ldcs.dai.DaiPortInterface;
import com.elyxor.xeros.ldcs.dai.DaiPortManager;
import com.elyxor.xeros.ldcs.dai.PortManagerInterface;

public class SerialListener {
	
	final static Logger logger = LoggerFactory.getLogger(SerialListener.class);
	    
	static volatile boolean portFinderRunning;

	public static void main(String[] args) {
		(new Thread(new commandListener())).start();
		(new Thread(new PortFinder())).start();
	}
	
    static class commandListener implements Runnable {

        private PortManagerInterface _portManager = null;
        
        public commandListener setPortManager(PortManagerInterface portManager) {
        	_portManager = portManager;
        	return this;
        }
        
        public PortManagerInterface getPortManager() {
        	if (null == _portManager) {
        		_portManager = new DaiPortManager();
        	}
        	return _portManager;
        }

        public void run() {
    		logger.info("Listening to command line inputs");
    		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    		String line = "";
    		
			while (line.equalsIgnoreCase("quit") == false) {
				try {
					line = in.readLine();
					processCommand(line, in, System.out);
				} catch (IOException e) {logger.warn("Failed to readline from command line", e);}
    		}
    	}

        /* 
         * TODO: Figure out why this is trying to both output a list of ports and 
         * add them to the active port list
         */
    	public void listPorts(PrintStream out) {
//			int i = 1;
			for (String port : getPortManager().getPortList()) {
				out.println(port);
//				activeDaiPorts.put(i, port);
//				out.println(i + " - " + daiPrefix + port.getDaiNum());
//				i++;
			}    		
    	}
    	
    	public void stop(PrintStream out) {
    		out.println("Stopping");
    		shutdownListener();
    	}

    	public void getIdForPort(BufferedReader in, PrintStream out) {

			int port = this.readIntFromBufferedReader(in, out);
			DaiPortInterface daiPort = findDaiPort(port);
			if (null != daiPort) {
				int bufferint = daiPort.getRemoteDaiId();		
				out.println(Integer.toString(bufferint));
    		}
		}

    	public void setIdForPort(BufferedReader in, PrintStream out) {
    		
			int port = this.readIntFromBufferedReader(in, out);
			DaiPortInterface daiPort = findDaiPort(port);
			if (null != daiPort) {
    			String result = daiPort.setRemoteDaiId(port);
    			out.println(result);
    		}
    	}
    	
    	public void sendStdRequest(BufferedReader in, PrintStream out) {
    		
			int port = this.readIntFromBufferedReader(in, out);
			DaiPortInterface daiPort = findDaiPort(port);
			if (null != daiPort) {
				String result = daiPort.sendStdRequest();
				out.println(result);
			}
		}

    	public void sendX(BufferedReader in, PrintStream out) {
			
			int port = this.readIntFromBufferedReader(in, out);
			DaiPortInterface daiPort = findDaiPort(port);
			if (null != daiPort) {
				String result = daiPort.sendXerosRequest();
				out.println(result);
			}
    	}
    	
    	public void clearPortBuffer(BufferedReader in, PrintStream out) {		
    		
			int port = this.readIntFromBufferedReader(in, out);
			DaiPortInterface daiPort = findDaiPort(port);
			if (null != daiPort) {
				String result = daiPort.clearPortBuffer();
				out.println(result);
			}
		}

    	public void setClock(BufferedReader in, PrintStream out) {
			int port = this.readIntFromBufferedReader(in, out);
			DaiPortInterface daiPort = findDaiPort(port);
			if (null != daiPort) {
				String result = daiPort.setClock();
				out.println(result);
			}
    	}
    	
    	public void readClock(BufferedReader in, PrintStream out) {
			int port = this.readIntFromBufferedReader(in, out);
			DaiPortInterface daiPort = findDaiPort(port);
			if (null != daiPort) {
				String result = daiPort.readClock();
				out.println(result);
			}
    	}
    	
    	private DaiPortInterface findDaiPort(int portNum) {
    		DaiPortInterface daiPort = null;
    		if (portNum > 0) {
    			daiPort = activeDaiPorts.get(portNum);
    		}
    		return daiPort;
    	}
    	
    	private int readIntFromBufferedReader(BufferedReader in, PrintStream out) {
    		int intVal = -1;
    		out.println("enter port number: ");
			String inputStr = "";
			try {
				inputStr = in.readLine();
				intVal = Integer.parseInt(inputStr);
			} catch (NumberFormatException nfe) {
				logger.warn("command coudn't parse int from user input "+ inputStr);
			    out.println("Please enter a number next time. " + inputStr + " is not a number.");
			} catch (IOException ioe) {
				logger.warn("command coudn't read int from input reader", ioe);
			    out.println("command couldn't read int from input reader. IOException: " + ioe.getMessage());			    		
			}
			
			return intVal;
    	}
    	
    	public void processCommand(String command, BufferedReader in, PrintStream out) {
    		
    		if (null == command || command.length() == 0) {
    			logger.warn("Received empty command. Nothing to do");
    			return;
    		}
    		
    		switch (command.toLowerCase()) {
	    		case "list": {
	    			this.listPorts(out);
	    			break;
	    		}
	    		case "stop": {
	    			this.stop(out);
	    			break;
	    		}
	    		case "getid": {
	    			this.getIdForPort(in, out);
	    			break;
	    		}
	    		case "setid": {
	    			this.setIdForPort(in, out);
	    			break;
	    		}
	    		case "sendstd": {
	    			this.sendStdRequest(in, out);
	    			break;
	    		}
	    		case "sendx": {
	    			this.sendX(in, out);
	    			break;
	    		}
	    		case "clear": {
	    			this.clearPortBuffer(in, out);
	    			break;
	    		}
	    		case "setclock": {
	    			this.setClock(in, out);
	    			break;
	    		}
	    		case "readClock": {
	    			this.readClock(in, out);
	    			break;
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
	            
    public static void shutdownListener() {
		portFinderRunning = false;
		logger.info("Stopped listening for new ports");
		for (Entry<String,DaiPortInterface> entry : portList.entrySet()) {
			DaiPortInterface port = entry.getValue();
			port.closePort();
		}
	}
    
/*    
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
    */
}


