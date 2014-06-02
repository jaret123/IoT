package com.elyxor.xeros.ldcs.dai;

import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.JobBuilder.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elyxor.xeros.ldcs.AppConfiguration;
import com.elyxor.xeros.ldcs.util.FileLogWriter;

import jssc.SerialPort;

public class PortManager implements PortManagerInterface, PortChangedListenerInterface {

	final static Logger logger = LoggerFactory.getLogger(PortManager.class);
	
	private String daiPrefix = AppConfiguration.getDaiName();
	
	private Path path = Paths.get("/home/will/ldcs/input"); //TODO set back to AppConfiguration.getLocalPath()
	
	boolean waterOnly = AppConfiguration.getWaterOnly();
	
	//quartz setup for scheduled task for water only
	SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
	Scheduler sched;
	
	Trigger waterOnlyTrigger = newTrigger()
			.withIdentity("waterOnlyTrigger")
			.withSchedule(dailyAtHourAndMinute(00,00))
		    .build();
	CronTrigger clockSetTrigger = newTrigger()
			.withIdentity("clockSetTrigger")
			.withSchedule(cronSchedule("0 0 1 ? * SUN"))
			.build();
	
	
	
	private static Map<String,DaiPortInterface> portList = new LinkedHashMap<String,DaiPortInterface>();

	PortFinderInterface _pf;
	private int nextDaiNum = 1;
	
	public void getPortFinder(PortFinderInterface pfi) {
		if (null != pfi) {
			_pf = pfi;
			_pf.addListener(this);
			logger.info("Started watching ports for changes");
		}
	}
		
	public boolean portAdded(String portName) {
		DaiPortInterface daiPort = new DaiPort(new SerialPort(portName), nextDaiNum, new FileLogWriter(path, daiPrefix+nextDaiNum+"Log.txt"), daiPrefix);
		String daiId = "";
		
		if (daiPort.openPort()) {
			daiId = daiPort.getRemoteDaiId();
			if (daiId == "0") {
				daiPort.setRemoteDaiId(nextDaiNum);
		    	logger.info("Assigned DAI ID "+daiPrefix+nextDaiNum+" to port "+portName);	
			}
			
			portList.put(portName, daiPort);
			nextDaiNum++;
			return true;
		}
		return false;
	}
	
	public boolean portRemoved(String portName) {
		DaiPortInterface daiPort = findDaiPort(portName);
		if (daiPort != null) {
			if (daiPort.closePort()) {
				portList.remove(daiPort);
				nextDaiNum = daiPort.getDaiNum(); 
			}
			// port close failed ?
			return false;
		}
		// port already removed
		return false;
	}
	
	public List<String> getPortList()  {
		List<String> portListOut = new ArrayList<String>();
		if (!portList.isEmpty()) {
			for (Entry<String,DaiPortInterface> entry : portList.entrySet()) {
				portListOut.add(entry.getKey() + ", " + daiPrefix + entry.getValue().getDaiNum());
			}
		}
		return portListOut;
	}
	
	public DaiPortInterface findDaiPort(String portName) {
		DaiPortInterface daiPort = null;
		if (!portName.isEmpty()) {
			daiPort = portList.get(portName);
		}
		return daiPort;
	}
	
	public DaiPortInterface findDaiPort(int id) {
		DaiPortInterface daiPort = null;
		if (id > 0) {
			for (Entry<String,DaiPortInterface> entry : portList.entrySet()) {
				if (entry.getValue().getDaiNum() == id) {
					daiPort = entry.getValue();
				}
			}
		}
		return daiPort;
	}
	
    public void shutdownListener() {
		_pf.setRunning(false);
		logger.info("Stopped listening for new ports");
		for (Entry<String,DaiPortInterface> entry : portList.entrySet()) {
			DaiPortInterface port = entry.getValue();
			port.closePort();
		}
	}
    
	public void startScheduler() {
		try {
			Scheduler sched = schedFact.getScheduler();
			JobDetail clockSetJob = newJob(ClockSetJob.class)
					.withIdentity("clockSetJob")
					.build();
			sched.scheduleJob(clockSetJob, clockSetTrigger);
			logger.info("scheduled clock set job, next fire time: "+clockSetTrigger.getNextFireTime().toString());
			if (waterOnly) {
				JobDetail waterOnlyJob = newJob(WaterOnlyJob.class)
						.withIdentity("waterOnlyJob")
						.build();
				sched.scheduleJob(waterOnlyJob, waterOnlyTrigger);
				logger.info("scheduled water only job, next fire time: "+waterOnlyTrigger.getNextFireTime().toString());

			}
			sched.start();
		} catch (Exception ex) {logger.warn("could not start scheduler",ex);}
	}
	
	public static class WaterOnlyJob implements Job {
		public WaterOnlyJob() {}
		public void execute(JobExecutionContext context) throws JobExecutionException {
			logger.info("Executing water only data collection");
			for (DaiPortInterface daiPort : portList.values()) {
				daiPort.sendStdRequest();
			}
		}
	}
	public static class ClockSetJob implements Job {
		public ClockSetJob() {}
		public void execute(JobExecutionContext context) throws JobExecutionException {
			logger.info("Executing clock set data collection");
			for (DaiPortInterface daiPort : portList.values()) {
				daiPort.setClock();
			}
		}
	} 
}
