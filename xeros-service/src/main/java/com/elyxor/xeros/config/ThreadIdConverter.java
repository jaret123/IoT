package com.elyxor.xeros.config;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class ThreadIdConverter extends ClassicConverter {

	@Override
	public String convert(ILoggingEvent logEvent) {		
		return Long.toString(Thread.currentThread().getId());
	}

}
