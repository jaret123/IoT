package com.elyxor.xeros.ldcs.dai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DaiPortManager implements PortManagerInterface {

	private Map<String,DaiPortInterface> portList = new LinkedHashMap<String,DaiPortInterface>();
	private Map<Integer, DaiPortInterface> activeDaiPorts = new HashMap<Integer, DaiPortInterface>();
	
	public List<String> getPortList()  {
		return new ArrayList<String>();
	}
}
