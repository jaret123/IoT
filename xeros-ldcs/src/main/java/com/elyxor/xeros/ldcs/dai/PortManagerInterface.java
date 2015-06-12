package com.elyxor.xeros.ldcs.dai;

import java.util.List;
import java.util.Map;

public interface PortManagerInterface {
	public void getPortFinder(PortFinderInterface pfi);
	public List<String> getPortNameList();
	public DaiPortInterface findDaiPort(String s);
	public DaiPortInterface findDaiPort(int id);
	public void shutdownListener();
    public void initThingWorxClient();
    public JobSchedulerInterface getJobScheduler();
    public Map<String, DaiPortInterface> getPortList();
}
