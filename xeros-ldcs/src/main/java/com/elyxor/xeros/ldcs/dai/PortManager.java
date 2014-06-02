package com.elyxor.xeros.ldcs.dai;

import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.JobBuilder.*;
import static org.quartz.JobKey.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
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
	
	private Map<String,DaiPortInterface> portList = new LinkedHashMap<String,DaiPortInterface>();

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
			
//			if (waterOnly) {startWaterOnly(daiPort);}
			startClockSchedule(daiPort);
			
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
//				stopWaterOnly(daiPort);
//				stopClockSchedule(daiPort);
				//need stopWaterOnly & stop clock once implemented
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
			sched.start();
		} catch (Exception ex) {logger.warn("could not start scheduler",ex);}
	}

    
    private void startWaterOnly (DaiPortInterface daiPort) {
		try {
	    	JobDetail job = newJob(WaterOnlyJob.class)
    				.withIdentity(daiPrefix+daiPort.getDaiNum(), "waterOnly")
					.build();
			job.getJobDataMap().put("daiPort", daiPort);
			sched.scheduleJob(job, waterOnlyTrigger);
		} catch (Exception ex) {logger.warn("could not start schedule for water only", ex);}
    }
    private void stopWaterOnly(DaiPortInterface daiPort) {
    	try {    		
    		sched.deleteJob(jobKey(daiPrefix+daiPort.getDaiNum(), "waterOnly"));
		} catch (Exception ex) {logger.warn("could not stop schedule for water only",ex);}
	}

    private void startClockSchedule (DaiPortInterface daiPort) {
    	try {
    		JobDetail job = newJob(ClockSetJob.class)
    				.withIdentity(daiPrefix+daiPort.getDaiNum(), "clock")
    				.build();
    		job.getJobDataMap().put("daiPort", daiPort);
    		sched.scheduleJob(job, clockSetTrigger);
    	} catch (Exception ex) {logger.warn("could not start schedule for clock set",ex);}
    }
    private void stopClockSchedule(DaiPortInterface daiPort) {
    	try {    		
    		sched.deleteJob(jobKey(daiPrefix+daiPort.getDaiNum(), "clock"));
		} catch (Exception ex) {logger.warn("could not stop schedule for water only",ex);}
	}
}
