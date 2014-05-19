package com.elyxor.xeros.ldcs;

import com.elyxor.xeros.ldcs.SerialListener.PortFinder;
import com.elyxor.xeros.ldcs.SerialListener.commandListener;

public class CollectionMain {

	public static void main(String[] args) {
		try {
			
			(new Thread(new commandListener())).start();
			(new Thread(new PortFinder())).start();
			
			new FileWatcher().watch();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
