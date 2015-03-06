package com.elyxor.xeros.ldcs.dai;

import com.elyxor.xeros.ldcs.AppConfiguration;
import com.elyxor.xeros.ldcs.util.FileLogWriter;
import jssc.SerialPort;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.DateBuilder.IntervalUnit;
import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class PortManager implements PortManagerInterface, PortChangedListenerInterface {

	final static Logger logger = LoggerFactory.getLogger(PortManager.class);
	private String daiPrefix = AppConfiguration.getDaiName();
	Integer waterOnly = AppConfiguration.getWaterOnly();
	
	private String currentDir = Paths.get("").toAbsolutePath().getParent().toString();
	private Path path = Paths.get(currentDir, "/input");
	
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
        logger.info("Found new USB device: "+portName);
		if (waterOnly == 2) {
			WaterMeterPortInterface waterMeterPort = new WaterMeterPort(new SerialPort(portName), nextDaiNum, new FileLogWriter(path, daiPrefix+nextDaiNum+"Log.txt"), daiPrefix, null);
			if (waterMeterPort.openPort()) {
				String id =  waterMeterPort.initRequest();
                if (id == null) {
                    waterMeterPort.closePort();
                    return false;
                }
                waterMeterPort.setLogWriter(new FileLogWriter(path, daiPrefix + waterMeterPort.getWaterMeterId() + "Log.txt"));
				portList.put(portName, waterMeterPort);
				nextDaiNum++;
                logger.info("Added EK meter with ID: " + daiPrefix + waterMeterPort.getWaterMeterId());
				return true;
			}
		}
		DaiPortInterface daiPort = new DaiPort(new SerialPort(portName), nextDaiNum, new FileLogWriter(path, daiPrefix+nextDaiNum+"Log.txt"), daiPrefix);
		String daiId = null;
		String newId = null;
		int retryCounter = 0;
        int daiIdInt = 0;
		
		if (daiPort.openPort()) {
            while (retryCounter < 3) {
			    daiId = daiPort.getRemoteDaiId();
			    if (daiId != null) {
                    try {
                        daiIdInt = Integer.parseInt(daiId.trim());
                    } catch (NumberFormatException e) {
                        logger.warn("failed to parse integer", e.getMessage());
                    }
                    logger.info("DAI ID is: '" +daiIdInt+"'");
//                    if (daiId.equals("0")) {
//                        newId = daiPort.setRemoteDaiId(nextDaiNum);
//                        if (newId != null && !newId.equals("0")) {
//                            logger.info("Assigned DAI ID " + daiPrefix + nextDaiNum + " to port " + portName);
//                            nextDaiNum++;
//                            break;
//                        }
//                    }
                    if (daiIdInt > 0) {
                        daiPort.setDaiNum(daiIdInt);
                        logger.info("Found existing DAI with ID "+daiPrefix+daiIdInt+" on port"+portName);
                        nextDaiNum = daiIdInt + 1;
                        break;
                    }
                }
                retryCounter++;
			}
            if (waterOnly == 1||waterOnly==3) {
                daiPort.initWaterRequest();
            }
            if (daiId == null || daiId.equals("-1")) {
                daiPort.closePort();
                return false;
            }
            daiPort.setLogWriter(new FileLogWriter(path, daiPrefix+daiPort.getDaiNum()+"Log.txt"));
			portList.put(portName, daiPort);
			return true;
		}
		return false;
	}
	
	public boolean portRemoved(String portName) {
        logger.info("USB Device removed: "+portName);
		DaiPortInterface daiPort = findDaiPort(portName);
		if (daiPort != null) {
			if (daiPort.closePort()) {
				portList.remove(daiPort.getSerialPort().getPortName());
				nextDaiNum = daiPort.getDaiNum();
                return true;
			}
			// port close failed
			return false;
		}
		// port already removed
		return false;
	}
	
	public List<String> getPortList()  {
		List<String> result = new ArrayList<String>();
		if (!portList.isEmpty()) {
			for (Entry<String,DaiPortInterface> entry : portList.entrySet()) {
                if (waterOnly == 2)
                    result.add(entry.getKey() + ", " + ((WaterMeterPortInterface)entry.getValue()).getWaterMeterId());
				result.add(entry.getKey() + ", " + daiPrefix + entry.getValue().getDaiNum());
			}
		}
		return result;
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
    
	//quartz setup for scheduled tasks for water only and clock set
	SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
	Scheduler sched;
    Trigger waterOnlyTrigger = newTrigger()
            .withIdentity("waterOnlyTrigger")
            .startAt(futureDate(3, IntervalUnit.MINUTE))
            .withSchedule(simpleSchedule()
                    .withIntervalInMinutes(15)
                    .repeatForever())
            .build();
    Trigger waterOnlyXerosTrigger = newTrigger()
            .withIdentity("waterOnlyXerosTrigger")
            .startAt(futureDate(3, IntervalUnit.MINUTE))
            .withSchedule(simpleSchedule()
                    .withIntervalInMinutes(15)
                    .repeatForever())
            .build();
	CronTrigger clockSetTrigger = newTrigger()
			.withIdentity("clockSetTrigger")
			.withSchedule(cronSchedule("0 0 1 ? * SUN"))
			.build();
	CronTrigger pingTrigger = newTrigger()
			.withIdentity("pingTrigger")
			.withSchedule(cronSchedule("0 0 */1 * * ?"))
			.build();
    Trigger waterOnlyManualTrigger = newTrigger()
            .withIdentity("waterOnlyManualTrigger")
            .startAt(futureDate(2, IntervalUnit.MINUTE))
            .withSchedule(simpleSchedule()
                    .withIntervalInMinutes(1)
                    .repeatForever())
            .build();
    Trigger machineStatusTrigger = newTrigger()
            .withIdentity("machineStatusTrigger")
            .startAt(futureDate(5, IntervalUnit.MINUTE))
            .withSchedule(simpleSchedule()
                    .withIntervalInMinutes(10)
                    .repeatForever())
            .build();
    Trigger machineStatusEkTrigger = newTrigger()
            .withIdentity("machineStatusEkTrigger")
            .startAt(futureDate(1, IntervalUnit.MINUTE))
            .withSchedule(simpleSchedule()
                    .withIntervalInMinutes(10)
                    .repeatForever())
            .build();



    public void startScheduler() {
		try {
			sched = schedFact.getScheduler();
//			JobDetail pingJob = newJob(PingJob.class) //always set up ping
//					.withIdentity("pingJob")
//					.build();
//			sched.scheduleJob(pingJob, pingTrigger);
//			logger.info("scheduled ping job, next fire time: "+pingTrigger.getNextFireTime().toString());
			if (waterOnly==2) { //DAQ-less water only, no clock set necessary
				JobDetail waterOnlyManualJob = newJob(WaterOnlyManualJob.class)
						.withIdentity("waterOnlyManualJob")
						.build();
				sched.scheduleJob(waterOnlyManualJob, waterOnlyManualTrigger);
                logger.info("scheduled DAQless water only, next fire time: "+waterOnlyManualTrigger.getNextFireTime().toString());

                JobDetail machineStatusEkJob = newJob(MachineStatusEkJob.class)
                        .withIdentity("machineStatusEkJob")
                        .build();
                sched.scheduleJob(machineStatusEkJob, machineStatusEkTrigger);
                logger.info("scheduled ek machine status, next fire time: " + machineStatusEkTrigger.getNextFireTime().toString());

				sched.start();
				return;
			}
			//setup clock set if using a DAQ
			JobDetail clockSetJob = newJob(ClockSetJob.class)
					.withIdentity("clockSetJob")
					.build();
			sched.scheduleJob(clockSetJob, clockSetTrigger);
			logger.info("scheduled clock set job, next fire time: "+clockSetTrigger.getNextFireTime().toString());

            JobDetail machineStatusJob = newJob(MachineStatusJob.class)
                    .withIdentity("machineStatusJob")
                    .build();
            sched.scheduleJob(machineStatusJob, machineStatusTrigger);
			logger.info("schedule machine status job, next fire time: "+machineStatusTrigger.getNextFireTime().toString());

			if (waterOnly==1) { //water only request if using DAQ and water only
				JobDetail waterOnlyJob = newJob(WaterOnlyJob.class)
						.withIdentity("waterOnlyJob")
						.build();
				sched.scheduleJob(waterOnlyJob, waterOnlyTrigger);
				logger.info("scheduled water only job, next fire time: "+waterOnlyTrigger.getNextFireTime().toString());
			}
            if (waterOnly==3) { //water only request using the Digital side of the DAQ
                JobDetail waterOnlyXerosJob = newJob(WaterOnlyXerosJob.class)
                        .withIdentity("waterOnlyXerosJob")
                        .build();
                sched.scheduleJob(waterOnlyXerosJob, waterOnlyXerosTrigger);
                logger.info("scheduled water only job, next fire time: "+waterOnlyXerosTrigger.getNextFireTime().toString());

            }
			sched.start();
		} catch (Exception ex) {logger.warn("could not start scheduler",ex);}
	}
	
	public static class WaterOnlyJob implements Job {
		public WaterOnlyJob() {}
		public void execute(JobExecutionContext context) throws JobExecutionException {
			logger.info("Executing water only data collection");
			String buffer = null;
			for (DaiPortInterface daiPort : portList.values()) {
                try {
                    buffer = daiPort.sendWaterRequest();
                } catch (Exception ex) {logger.warn("unable to complete water meter request", ex);}

                logger.info(buffer);
                long[] result = daiPort.calculateWaterLog(buffer);
                if (result!=null) {
                    daiPort.writeWaterOnlyLog(result);
                }
			}
		}
	}
	public static class WaterOnlyManualJob implements Job {
		public WaterOnlyManualJob() {}
		public void execute(JobExecutionContext context) throws JobExecutionException {
			logger.info("Executing water only data collection");
			String buffer = null;
			for (DaiPortInterface daiPort : portList.values()) {
				buffer = daiPort.sendRequest();
				if (buffer != null && !buffer.equals("")) {
					daiPort.writeLogFile(buffer);
				}
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
	public static class PingJob implements Job {
		public PingJob() {}
		public void execute(JobExecutionContext context) throws JobExecutionException {
			logger.info("Executing ping");
			for (DaiPortInterface daiPort : portList.values()) {
				daiPort.ping();
			}
		}
	}
    public static class MachineStatusJob implements Job {
        public MachineStatusJob() {}
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("Executing machine status update");
            for (DaiPortInterface daiPort : portList.values()) {
                daiPort.sendMachineStatus();
            }
        }
    }
    public static class MachineStatusEkJob implements Job {
        public MachineStatusEkJob() {}
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("Executing EK machine status update");
            for (DaiPortInterface daiPort : portList.values()) {
                daiPort.sendMachineStatus();
            }
        }
    }
    public static class WaterOnlyXerosJob implements Job {
        public WaterOnlyXerosJob() {}
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("Executing water only data collection");
            String buffer = null;
            for (DaiPortInterface daiPort : portList.values()) {
                try {
                    buffer = daiPort.sendXerosWaterRequest();
                } catch (Exception ex) {logger.warn("unable to complete water meter request", ex);}

                logger.info(buffer);
                long[] result = daiPort.calculateWaterLog(buffer);
                if (result!=null) {
                    daiPort.writeWaterOnlyXerosLog(result);
                }
            }
        }
    }
}
