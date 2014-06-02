package com.elyxor.xeros.ldcs.dai;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.quartz.SchedulerException;

import jssc.SerialPortList;

public class PortFinder implements Runnable, PortFinderInterface {
	
	PortChangedListenerInterface _portListener;
	public boolean portFinderRunning;
	
	public void addListener(PortChangedListenerInterface pcli) {		
		if (null != pcli) {
			_portListener = pcli;
			new Thread(this).start();
		}
	}

	public void run() {
		portFinderRunning = true;
		List<String> activeLocalPorts = new LinkedList<String>();

		while (portFinderRunning) {
			
			List<String> newPorts = Arrays.asList(SerialPortList.getPortNames());
			
			for (String portName : newPorts) {
				if (!activeLocalPorts.contains(portName))
					try {
						_portListener.portAdded(portName);
					} catch (SchedulerException e) {
						e.printStackTrace();
					}
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
