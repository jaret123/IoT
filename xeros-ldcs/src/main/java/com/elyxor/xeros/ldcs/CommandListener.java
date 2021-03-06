package com.elyxor.xeros.ldcs;

import com.elyxor.xeros.ldcs.dai.DaiPortInterface;
import com.elyxor.xeros.ldcs.dai.PortFinder;
import com.elyxor.xeros.ldcs.dai.PortManager;
import com.elyxor.xeros.ldcs.dai.PortManagerInterface;
import com.elyxor.xeros.ldcs.reliagate.MockReliagatePortManager;
import com.elyxor.xeros.ldcs.reliagate.ReliagatePortManager;
import com.elyxor.xeros.ldcs.reliagate.ReliagatePortManagerInterface;
import com.elyxor.xeros.ldcs.thingworx.ThingWorxClient;
import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.common.SecurityClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class CommandListener implements Runnable {

	final static Logger logger = LoggerFactory.getLogger(CommandListener.class);

	private PortManagerInterface _portManager = null;
    private ThingWorxClient mClient = null;

	public static void main(String[] args) {
        CommandListener commandListener = new CommandListener();
        (new Thread(commandListener)).start();

        String portType = AppConfiguration.getPortType();
        Boolean thingworx = AppConfiguration.getThingWorx();
        Boolean isMock = AppConfiguration.getMock();

        if (isMock) {
            ReliagatePortManagerInterface mockPm = new MockReliagatePortManager();
            mockPm.setThingWorxClient(commandListener.initThingWorxClient());
            mockPm.init();

            return;
        }

        if (portType != null && portType.equals("reliagate")) {
            logger.info("Starting Reliagate Port Manager");
            ReliagatePortManagerInterface pm = new ReliagatePortManager();

            if (thingworx) {
                logger.info("Initializing Thingworx");

                pm.setThingWorxClient(commandListener.initThingWorxClient());
            }
            pm.init();
        } else {
            PortManagerInterface manager = commandListener.getPortManager();
            if (thingworx) {
                manager.setThingWorxClient(commandListener.initThingWorxClient());
            }
            manager.getJobScheduler().start();
            manager.getPortFinder(new PortFinder());
        }
	}

    public ThingWorxClient initThingWorxClient() {
        ClientConfigurator config = new ClientConfigurator();

        String url = AppConfiguration.getThingWorxUrl();
        String apiKey = AppConfiguration.getThingWorxApiKey();
        String daiName = AppConfiguration.getDaiName();

        if (url == null) {
            logger.warn("Could not initialize ThingWorx, no URL in ldcs.properties");
            return null;
        }
        if (apiKey == null) {
            logger.warn("Could not initialize ThingWorx, no API Key in ldcs.properties");
            return null;
        }
        // The uri for connecting to Thingworx
        config.setUri(url);

        // Reconnect every 15 seconds if a disconnect occurs or if initial connection cannot be made
        config.setReconnectInterval(15);

        // Set the security using an Application Key
        SecurityClaims claims = SecurityClaims.fromAppKey(apiKey);
        config.setSecurityClaims(claims);

        // Set the name of the client
        config.setName(daiName + "Gateway");

        // This client is a SDK
        config.setAsSDKType();
        if (mClient == null) {
            try {
                mClient = new ThingWorxClient(config);
            } catch (Exception e) {
                logger.warn("could not get client: ", e.getMessage());
            }
        }
        logger.info("Thingworx Initialized: Client: " + mClient);

        return mClient;
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
		for (String port : getPortManager().getPortNameList()) {
			out.println(port);
		}    		
	}
	
	public void stop(PrintStream out) {
		out.println("Stopping");
		_portManager.shutdownListener();
	}

	public void getIdForPort(BufferedReader in, PrintStream out) {
		out.println("enter port number: ");
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.getRemoteDaiId();		
			out.println("port id is: "+result);
		}
		else out.println("port not found.");
	}

	public void setIdForPort(BufferedReader in, PrintStream out) {
		out.println("enter port number: ");
		int port = this.readIntFromBufferedReader(in, out);
		out.println("enter new port id: ");
		int newPortId = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.setRemoteDaiId(newPortId);
			out.println("new port id is: "+result);
		}
		else out.println("port not found.");
	}
	
	public void sendStdRequest(BufferedReader in, PrintStream out) {
		out.println("enter port number: ");
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.sendStdRequest();
			out.println(result);
			daiPort.writeLogFile(result);
		}
		else out.println("port not found.");
	}

	public void sendX(BufferedReader in, PrintStream out) {
		out.println("enter port number: ");
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.sendXerosRequest();
			out.println(result);
			daiPort.writeLogFile(result);
		}
		else out.println("port not found.");
	}
		
	public void clearPortBuffer(BufferedReader in, PrintStream out) {		
		out.println("enter port number: ");
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.clearPortBuffer();
			out.println(result);
		}
		else out.println("port not found.");
	}

	public void setClock(BufferedReader in, PrintStream out) {
		out.println("enter port number: ");
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.setClock();
			out.println(result);
		}
		else out.println("port not found.");
	}
	
	public void readClock(BufferedReader in, PrintStream out) {
		out.println("enter port number: ");
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (null != daiPort) {
			String result = daiPort.readClock();
			out.println(result);
		}
		else out.println("port not found.");
	}
	public void readWaterMeter(BufferedReader in, PrintStream out) {
		out.println("enter port number: ");
		int port = this.readIntFromBufferedReader(in, out);
		DaiPortInterface daiPort = getPortManager().findDaiPort(port);
		if (daiPort != null) {
            try {
                String result = daiPort.sendWaterRequest();
                out.println(result);
            } catch (Exception ex) {logger.warn("unable to complete water request",ex);}
        }
		else out.println("port not found.");
	}
	private int readIntFromBufferedReader(BufferedReader in, PrintStream out) {
		int intVal = -1;
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
		
//		switch (command.toLowerCase()) {
//    		case "list": {
//    			this.listPorts(out);
//    			break;
//    		}
//    		case "stop": {
//    			this.stop(out);
//    			break;
//    		}
//    		case "getid": {
//    			this.getIdForPort(in, out);
//    			break;
//    		}
//    		case "setid": {
//    			this.setIdForPort(in, out);
//    			break;
//    		}
//    		case "sendstd": {
//    			this.sendStdRequest(in, out);
//    			break;
//    		}
//    		case "sendx": {
//    			this.sendX(in, out);
//    			break;
//    		}
//    		case "clear": {
//    			this.clearPortBuffer(in, out);
//    			break;
//    		}
//    		case "setclock": {
//    			this.setClock(in, out);
//    			break;
//    		}
//    		case "readClock": {
//    			this.readClock(in, out);
//    			break;
//    		}
//    		case "water": {
//    			this.readWaterMeter(in, out);
//    			break;
//    		}
//		}
	}
}
