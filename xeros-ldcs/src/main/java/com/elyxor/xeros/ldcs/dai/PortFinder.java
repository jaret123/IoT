package com.elyxor.xeros.ldcs.dai;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import jssc.SerialPort;
import jssc.SerialPortList;

public class PortFinder implements Runnable {
	
	PortChangedListenerInterface _portListener;
	public boolean portFinderRunning;
	
	public void addListener(PortChangedListenerInterface pcli) {		
		if (null != pcli) {
			_portListener = pcli;
		}
	}

	public void run() {
		portFinderRunning = true;
		List<String> activeLocalPorts = new LinkedList<String>();

		while (portFinderRunning) {
			List<String> newPorts = Arrays.asList(SerialPortList.getPortNames());
			
			for (String portName : newPorts) {
				if (!activeLocalPorts.contains(portName))
					_portListener.portAdded(portName);
			}
			for (String portName : activeLocalPorts) {
				if (!newPorts.contains(portName)) {
					_portListener.portRemoved(portName);
				}
			}
			activeLocalPorts = newPorts;
		}
	}
	
	public void setRunning(boolean running) {
		this.portFinderRunning = running;
	}
}
