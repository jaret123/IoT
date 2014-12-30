package com.elyxor.xeros.ldcs.dai;

import jssc.SerialPortList;
import org.quartz.SchedulerException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

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
        Pattern pattern = Pattern.compile("(ttyUSB)[0-9]{1,3}");
		while (portFinderRunning) {
            List<String> newPorts = Arrays.asList(SerialPortList.getPortNames());
//			List<String> newPorts = Arrays.asList(SerialPortList.getPortNames(pattern));

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
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
		}
	}
	
	public void setRunning(boolean running) {
		this.portFinderRunning = running;
	}
}
