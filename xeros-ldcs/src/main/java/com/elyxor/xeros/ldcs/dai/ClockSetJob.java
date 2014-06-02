package com.elyxor.xeros.ldcs.dai;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ClockSetJob implements Job {
	
	DaiPortInterface daiPort;
	
	public ClockSetJob() {
	}
	
	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap dataMap = context.getMergedJobDataMap();
		daiPort.setClock();
	}
	public void setDaiPort(DaiPortInterface dpi) {
		this.daiPort = dpi;
	}
}
