package com.elyxor.xeros.ldcs.dai;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by will on 6/12/15.
 */
public class JobScheduler implements JobSchedulerInterface {
    final static Logger logger = LoggerFactory.getLogger(JobScheduler.class);

    private static PortManagerInterface portManager;
    private Integer waterOnly;
    private SchedulerFactory schedFact;
    private Scheduler sched;

    public JobScheduler(PortManagerInterface manager, Integer waterOnlyFlag) {
        portManager = manager;
        this.waterOnly = waterOnlyFlag;
        this.schedFact = new StdSchedulerFactory();
    }

    @Override public void start() {
        try {
            sched = schedFact.getScheduler();
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

    //quartz setup for scheduled tasks for water only and clock set
    Trigger waterOnlyTrigger = newTrigger()
            .withIdentity("waterOnlyTrigger")
            .startAt(futureDate(3, DateBuilder.IntervalUnit.MINUTE))
            .withSchedule(simpleSchedule()
                    .withIntervalInMinutes(15)
                    .repeatForever())
            .build();
    Trigger waterOnlyXerosTrigger = newTrigger()
            .withIdentity("waterOnlyXerosTrigger")
            .startAt(futureDate(3, DateBuilder.IntervalUnit.MINUTE))
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
            .startAt(futureDate(2, DateBuilder.IntervalUnit.MINUTE))
            .withSchedule(simpleSchedule()
                    .withIntervalInMinutes(1)
                    .repeatForever())
            .build();
    Trigger machineStatusTrigger = newTrigger()
            .withIdentity("machineStatusTrigger")
            .startAt(futureDate(5, DateBuilder.IntervalUnit.MINUTE))
            .withSchedule(simpleSchedule()
                    .withIntervalInMinutes(10)
                    .repeatForever())
            .build();
    Trigger machineStatusEkTrigger = newTrigger()
            .withIdentity("machineStatusEkTrigger")
            .startAt(futureDate(1, DateBuilder.IntervalUnit.MINUTE))
            .withSchedule(simpleSchedule()
                    .withIntervalInMinutes(10)
                    .repeatForever())
            .build();

    public static class WaterOnlyJob implements Job {
        public WaterOnlyJob() {}
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("Executing water only data collection");
            String buffer = null;
            for (DaiPortInterface daiPort : portManager.getPortList().values()) {
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
            for (DaiPortInterface daiPort : portManager.getPortList().values()) {
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
            for (DaiPortInterface daiPort : portManager.getPortList().values()) {
                daiPort.setClock();
            }
        }
    }
    public static class PingJob implements Job {
        public PingJob() {}
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("Executing ping");
            for (DaiPortInterface daiPort : portManager.getPortList().values()) {
                daiPort.ping();
            }
        }
    }
    public static class MachineStatusJob implements Job {
        public MachineStatusJob() {}
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("Executing machine status update");
            for (DaiPortInterface daiPort : portManager.getPortList().values()) {
                daiPort.sendMachineStatus();
            }
        }
    }
    public static class MachineStatusEkJob implements Job {
        public MachineStatusEkJob() {}
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("Executing EK machine status update");
            for (DaiPortInterface daiPort : portManager.getPortList().values()) {
                daiPort.sendMachineStatus();
            }
        }
    }
    public static class WaterOnlyXerosJob implements Job {
        public WaterOnlyXerosJob() {}
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("Executing water only data collection");
            String buffer = null;
            for (DaiPortInterface daiPort : portManager.getPortList().values()) {
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
