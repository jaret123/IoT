package com.elyxor.xeros.ldcs.util;

import jssc.SerialPortEvent;

public interface SerialReaderInterface {
	public void serialEvent(SerialPortEvent event);
}