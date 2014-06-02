package com.elyxor.xeros.ldcs.dai;

import org.quartz.SchedulerException;

public interface PortChangedListenerInterface {
	public boolean portAdded(String s) throws SchedulerException;
	public boolean portRemoved(String s);
}
