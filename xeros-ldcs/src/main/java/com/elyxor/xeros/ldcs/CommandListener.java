package com.elyxor.xeros.ldcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elyxor.xeros.ldcs.dai.DaiPortInterface;
import com.elyxor.xeros.ldcs.dai.PortFinder;
import com.elyxor.xeros.ldcs.dai.PortManager;
import com.elyxor.xeros.ldcs.dai.PortManagerInterface;

public class CommandListener implements Runnable {

	final static Logger logger = LoggerFactory.getLogger(CommandListener.class);

	private PortManagerInterface _portManager = null;
    
	public static void main(String[] args) {
		CommandListener commandListener = new CommandListener();
		(new Thread(commandListener)).start();
		PortManagerInterface manager = commandListener.getPortManager();
		manager.getPortFinder(new PortFinder());
	}
	
    public CommandListener setPortManager(PortManagerInterface portManager) {
    	_portManager = portManager;
    	return this;
    }
    
    public PortManagerInterface getPortManager() {
    	if (null == _portManager) {
    		_portManager = new PortManager();
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
		_portManager.shutdownListener();
	}

	public void getIdForPort(BufferedReader in, PrintStream out) {

		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String buffer = daiPort.getRemoteDaiId();		
			out.println(buffer);
		}
	}

	public void setIdForPort(BufferedReader in, PrintStream out) {
		
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.setRemoteDaiId(port);
			out.println(result);
		}
	}
	
	public void sendStdRequest(BufferedReader in, PrintStream out) {
		
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.sendStdRequest();
			out.println(result);
		}
	}

	public void sendX(BufferedReader in, PrintStream out) {
		
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.sendXerosRequest();
			out.println(result);
		}
	}
	
	public void clearPortBuffer(BufferedReader in, PrintStream out) {		
		
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.clearPortBuffer();
			out.println(result);
		}
	}

	public void setClock(BufferedReader in, PrintStream out) {
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.setClock();
			out.println(result);
		}
	}
	
	public void readClock(BufferedReader in, PrintStream out) {
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.readClock();
			out.println(result);
		}
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
