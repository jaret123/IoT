package com.elyxor.xeros.ldcs.dai;

public interface PortFinderInterface {
	void addListener(PortChangedListenerInterface pcli);
	void setRunning(boolean bool);
}
