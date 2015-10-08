package com.elyxor.xeros.ldcs.dai;

import com.elyxor.xeros.ldcs.thingworx.ThingWorxClient;

import java.util.List;
import java.util.Map;

public interface PortManagerInterface {
	public void getPortFinder(PortFinderInterface pfi);
	public List<String> getPortNameList();
	public DaiPortInterface findDaiPort(String s);
	public DaiPortInterface findDaiPort(int id);
	public void shutdownListener();
    public JobSchedulerInterface getJobScheduler();
    public Map<String, DaiPortInterface> getPortList();
    public void setThingWorxClient(ThingWorxClient client);
}
