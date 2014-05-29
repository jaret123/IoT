package com.elyxor.xeros.ldcs.dai;

import java.util.List;

public interface PortManagerInterface {
	public List<String> getPortList();
	public DaiPortInterface findDaiPort(String s);
	public DaiPortInterface findDaiPort(int id);
	public void shutdownListener();
}
