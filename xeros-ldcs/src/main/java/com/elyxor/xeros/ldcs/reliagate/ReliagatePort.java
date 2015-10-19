package com.elyxor.xeros.ldcs.reliagate;

import com.elyxor.xeros.ldcs.AppConfiguration;
import com.elyxor.xeros.ldcs.thingworx.ThingWorxClient;
import com.elyxor.xeros.ldcs.thingworx.XerosWasherThing;
import com.elyxor.xeros.ldcs.util.FileLogWriter;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.net.TCPMasterConnection;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by will on 9/16/15.
 */
public class ReliagatePort implements PollingResultListener {

    private static final Logger logger = LoggerFactory.getLogger(ReliagatePort.class);
    private final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("HH : mm : ss");
    private final Integer portConfig = AppConfiguration.getPortConfig();

    private String daiPrefix;
    private Integer waterOnly;

    private static final int CONFIG_MIXED = 0;
    private static final int CONFIG_XEROS = 1;
    private static final int CONFIG_NON_XEROS = 2;

    private static final int PORT_NUM_MACHINE_1_DOOR_LOCK = 2;
    private static final int PORT_NUM_MACHINE_2_DOOR_LOCK = 10;
    private static final int PORT_NUM_MACHINE_1_COLD_WATER = 0;
    private static final int PORT_NUM_MACHINE_1_HOT_WATER = 1;
    private static final int PORT_NUM_MACHINE_2_COLD_WATER = 8;
    private static final int PORT_NUM_MACHINE_2_HOT_WATER = 9;

    private List<PortEvent> machine1EventLog;
    private List<PortEvent> machine2EventLog;

    private DateTime[] portStartTimes = new DateTime[16];
    private int[] portEventCounts = new int[16];

    private int machine1DoorLockTrue = 1;
    private int machine2DoorLockTrue = 1;

    private boolean mListening = true;
    private boolean machine1Started;
    private boolean machine2Started;
    private boolean mIsMock;

    private TCPMasterConnection mConnection;
    private Thread mThread;

    private PrintWriter mOut;

    private int mCount = 16;

    private String currentDir = Paths.get("").toAbsolutePath().getParent().toString();
    private Path path = Paths.get(currentDir, "/input");
    private File mFile = new File(path.toString(), "output.txt");

    private FileLogWriter mWriter;
    private FileLogWriter mMachine1WaterLogWriter;
    private FileLogWriter mMachine2WaterLogWriter;

    private List<List<PortEvent>> currentEvents = new ArrayList<List<PortEvent>>(mCount);

    private ThingWorxClient mClient;
    private XerosWasherThing mMachine1Thing;
    private XerosWasherThing mMachine2Thing;

    public ReliagatePort(TCPMasterConnection connection, int portNum, ThingWorxClient client) {
        logger.info("Starting Reliagate Port Number: " + portNum);
        this.mConnection = connection;
        this.mClient = client;
        initList();
        initConfig();
        daiPrefix = AppConfiguration.getDaiName() + portNum;
        mWriter = new FileLogWriter(path, daiPrefix+"Log.txt");
        machine1EventLog = new ArrayList<PortEvent>();
        machine2EventLog = new ArrayList<PortEvent>();
        waterOnly = AppConfiguration.getWaterOnly();

        if (waterOnly == 1) {
            mMachine1WaterLogWriter = new FileLogWriter(path, "/waterMeters/" + daiPrefix + "-Machine1Water-Log.txt");
        }
        if (waterOnly == 3) {
            mMachine2WaterLogWriter = new FileLogWriter(path, "/waterMeters/" + daiPrefix + "-Machine2Water-Log.txt");
        }
    }

    private void initConfig() {
        String xeros1Prefix = daiPrefix + "Xeros1";
        String xeros2Prefix = daiPrefix + "Xeros2";
        String std1Prefix = daiPrefix + "Std1";
        String std2Prefix = daiPrefix + "Std2";
        switch (portConfig) {
            case CONFIG_MIXED:
                machine1DoorLockTrue = 0;
                machine2DoorLockTrue = 1;

                mMachine1Thing = new XerosWasherThing(xeros1Prefix, xeros1Prefix, xeros1Prefix, mClient);
                mMachine2Thing = new XerosWasherThing(std1Prefix, std1Prefix, std1Prefix, mClient);
                break;
            case CONFIG_XEROS:
                machine1DoorLockTrue = 0;
                machine2DoorLockTrue = 0;

                mMachine1Thing = new XerosWasherThing(xeros1Prefix, xeros1Prefix, xeros1Prefix, mClient);
                mMachine2Thing = new XerosWasherThing(xeros2Prefix, xeros2Prefix, xeros2Prefix, mClient);
                break;
            case CONFIG_NON_XEROS:
                machine1DoorLockTrue = 1;
                machine2DoorLockTrue = 1;

                mMachine1Thing = new XerosWasherThing(std1Prefix, std1Prefix, std1Prefix, mClient);
                mMachine2Thing = new XerosWasherThing(std2Prefix, std2Prefix, std2Prefix, mClient);
                break;
            default:
                machine1DoorLockTrue = 0;
                machine2DoorLockTrue = 0;

                mMachine1Thing = new XerosWasherThing(xeros1Prefix, xeros1Prefix, xeros1Prefix, mClient);
                mMachine2Thing = new XerosWasherThing(std1Prefix, std1Prefix, std1Prefix, mClient);
                break;
        }
        try {
            if (mMachine1Thing != null) {
                mClient.bindThing(mMachine1Thing);
            }
            if (mMachine2Thing != null) {
                mClient.bindThing(mMachine2Thing);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread thread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                    mClient.start();
                    Thread.sleep(4000);
                } catch (Exception e) {
                    logger.warn("could not start client: ", e.toString());
                }
            }
        };
        thread.start();
    }

    private void initList() {
        for (int i = 0; i < mCount; i++) {
            currentEvents.add(new ArrayList<PortEvent>());
        }

    }

    public boolean startPolling(boolean isMock) {
        logger.info("Starting Reliagate Port Polling.");

        if (waterOnly == 1 || waterOnly == 3) {
            mThread = new Thread(new WaterOnlyPollingRunnable(this, waterOnly));
            mThread.start();
        } else {

            mIsMock = isMock;
            if (isMock) {
                mThread = new Thread(new PollingRunnable(this, mConnection, isMock));
                mThread.start();
                return true;
            }
            if (mThread == null) {
                if (mConnection == null) {
                    logger.warn("Polling", "Failed to start, no connection");
                    return true;
                }

                mThread = new Thread(new PollingRunnable(this, mConnection, isMock));
                mThread.start();
                logger.info("Polling Started.");

            }
        }
        return false;
    }

    public TCPMasterConnection getConnection() {
        return mConnection;
    }

    public void setConnection(TCPMasterConnection connection) {
        mConnection = connection;
    }

    public boolean isListening() {
        return mListening;
    }

    public void setListening(boolean listening) {
        mListening = listening;
    }

    @Override public void onPortChanged(int portNum, int newValue) {

        //first check if port is a door lock, if so, it may indicate the beginning or end of a cycle

        //if door lock for machine 1 or 2 is now locked, cycle has started. Write any old events, clear the event log and the water meters,
        //and begin tracking the cycle
        if (portNum == PORT_NUM_MACHINE_1_DOOR_LOCK) {
            logger.info("Machine 1 Door Lock Event");

            if (newValue == machine1DoorLockTrue) {
                logger.info("Machine 1 Door Lock Event - Started");
                machine1Started = true;
                if (!machine1EventLog.isEmpty()) {
                    logger.info("Machine 1 Writing Log: "+machine1EventLog);

                    writeEventLog(machine1EventLog, 1);
                }
                clearWaterCounter(1);
                portStartTimes[portNum] = new DateTime();
            } else {
                logger.info("Machine 1 Door Lock Event - Stopped");

                machine1Started = false;
                if (portStartTimes[portNum] != null) {
                    logger.info("Machine 1 Writing Log: "+machine1EventLog);
                    machine1EventLog.add(createCycleEvent(portNum));
                    writeEventLog(machine1EventLog, 1);
                }
            }
        } else if (portNum == PORT_NUM_MACHINE_2_DOOR_LOCK) {
            logger.info("Machine 2 Door Lock Event");

            if (newValue == machine2DoorLockTrue) {
                logger.info("Machine 2 Door Lock Event - Started");

                machine2Started = true;
                if (!machine2EventLog.isEmpty()) {
                    logger.info("Machine 1 Writing Log: "+machine1EventLog);
                    writeEventLog(machine2EventLog, 2);
                }
                clearWaterCounter(2);
                portStartTimes[portNum] = new DateTime();
            } else {
                logger.info("Machine 1 Door Lock Event - Stopped");

                machine2Started = false;
                if (portStartTimes[portNum] != null) {
                    logger.info("Machine 1 Writing Log: "+machine1EventLog);
                    machine2EventLog.add(createCycleEvent(portNum));
                    writeEventLog(machine2EventLog, 2);
                }
            }
        } else if (newValue == 1) {
            logger.info("Other Event - Started");

            portStartTimes[portNum] = new DateTime();
        } else if (newValue == 0) {
            logger.info("Other Event - Stopped");

            if (portStartTimes[portNum] != null) {
                if (portNum < 8) {
                    machine1EventLog.add(createCycleEvent(portNum));
                } else {
                    machine2EventLog.add(createCycleEvent(portNum));
                }
            }
        }
    }

    private PortEvent createCycleEvent(int portNum) {
        DateTime lastEventTime = portStartTimes[portNum];
        DateTime now = new DateTime();
        long duration = now.getMillis() - lastEventTime.getMillis();

        PortEvent event = new PortEvent(portNum, EventType.CYCLE_EVENT, lastEventTime, duration);
        return event;
    }

    private String createEventString(PortEvent event) {
        String format = getFormatForPortNum(event.getPortNum());
        String eventTime = event.getTimestamp().toString(timeFormatter);
        Period period = new Period(event.getDuration());
        int hours = period.getHours();
        int minutes = period.getMinutes();
        int seconds = period.getSeconds();
        int tenths = period.getMillis() / 100;

        String result = String.format(format, eventTime, hours, minutes, seconds, tenths);
        return result;
    }

    private String getFormatForPortNum(int portNum) {
        if (portNum == 2 || portNum == 10) {
            return "%1$s , %2$d : %3$d : %4$d.%5$d ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, " +
                    "00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, ";
        } else if (portNum == 3 || portNum == 11) {
            return "00 : 00 : 00 , 0 : 0 : 0.0 ,, %1$s , %2$d : %3$d : %4$d.%5$d ,, " +
                    "00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, ";
        } else if (portNum == 4 || portNum == 12) {
            return "00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, %1$s , %2$d : %3$d : %4$d.%5$d ,, " +
                    "00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, ";
        } else if (portNum == 5 || portNum == 13) {
            return "00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, " +
                    "%1$s , %2$d : %3$d : %4$d.%5$d ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, ";
        } else if (portNum == 6 || portNum == 14) {
            return "00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, " +
                    "00 : 00 : 00 , 0 : 0 : 0.0 ,, %1$s , %2$d : %3$d : %4$d.%5$d ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, ";
        } else if (portNum == 7 || portNum == 15) {
            return "00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, " +
                    "00 : 00 : 00 , 0 : 0 : 0.0 ,, 00 : 00 : 00 , 0 : 0 : 0.0 ,, %1$s , %2$d : %3$d : %4$d.%5$d ,, ";
        } else {
            return "";
        }
    }

    private void writeEventLog(List<PortEvent> eventLog, int machineNum){
        logger.info("Starting Event Log Writing");

        StringBuilder sb = new StringBuilder();
        sb.append(daiPrefix);
        sb.append(" , ");
        if (machineNum == 1) {
            sb.append("Xeros , ");
        } else if (machineNum == 2) {
            sb.append("Std , ");
        }

        sb.append("\n");
        sb.append("File Write Time: , ");
        sb.append(new DateTime().toString(timeFormatter));
        sb.append("\n\n");

        for (int i = 0; i < eventLog.size(); i++) {
            PortEvent event = eventLog.get(i);
            sb.append(i);
            sb.append(" ,, ");
            sb.append(createEventString(event));
            sb.append("\n\n\n");
        }

        addEventCounts(sb, machineNum);
        sb.append("\n\n");
        addWaterMeters(sb, machineNum);
        File file = null;
        try {
            logger.info("Writing Event Log: "+sb.toString());

            file = mWriter.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file != null) {
            if (machineNum == 1) {
                mMachine1Thing.parseLogToThingWorx(file);
            } else if (machineNum == 2) {
                mMachine2Thing.parseLogToThingWorx(file);
            }
        }
        eventLog.clear();
    }

    private void addWaterMeters(StringBuilder sb, int machineNum) {
        if (machineNum == 1) {
            int[] waterMeters;
            if (!mIsMock) {
                waterMeters = getMachine1WaterReadings();
            } else {
                waterMeters = new int[]{250, 150};
            }
            sb.append("WM 0:  , ");
            sb.append(waterMeters[0]);
            sb.append(" , ");
            sb.append(waterMeters[0]);
            sb.append(" , ");
            sb.append(waterMeters[0]);
            sb.append(" , ");
            sb.append("\n\n");
            sb.append("WM 1:  , ");
            sb.append(waterMeters[1]);
            sb.append(" , ");
            sb.append(waterMeters[1]);
            sb.append(" , ");
            sb.append(waterMeters[1]);
            sb.append("\n");

            clearWaterCounter(1);

        } else if (machineNum == 2) {
            int[] waterMeters;
            if (!mIsMock) {
                waterMeters = getMachine2WaterReadings();
            } else {
                waterMeters = new int[]{500, 650};
            }
            sb.append("WM 2:  , ");
            sb.append(waterMeters[0]);
            sb.append(" , ");
            sb.append(waterMeters[0]);
            sb.append(" , ");
            sb.append(waterMeters[0]);
            sb.append(" , ");
            sb.append("\n\n");
            sb.append("WM 3:  , ");
            sb.append(waterMeters[1]);
            sb.append(" , ");
            sb.append(waterMeters[1]);
            sb.append(" , ");
            sb.append(waterMeters[1]);
            sb.append("\n");

            clearWaterCounter(2);

        }
    }

    private void addEventCounts(StringBuilder sb, int machineNum) {
        if (machineNum == 1) {
            sb.append(portEventCounts[2]);
            sb.append(" , ");
            sb.append(portEventCounts[3]);
            sb.append(" , ");
            sb.append(portEventCounts[4]);
            sb.append(" , ");
            sb.append(portEventCounts[5]);
            sb.append(" , ");
            sb.append(portEventCounts[6]);
            sb.append(" , ");
            sb.append(portEventCounts[7]);
            sb.append(" , ");
            for (int i = 2; i < 8; i++) {
                portEventCounts[i] = 0;
            }
        } else if (machineNum == 2) {
            sb.append(portEventCounts[10]);
            sb.append(" , ");
            sb.append(portEventCounts[11]);
            sb.append(" , ");
            sb.append(portEventCounts[12]);
            sb.append(" , ");
            sb.append(portEventCounts[13]);
            sb.append(" , ");
            sb.append(portEventCounts[14]);
            sb.append(" , ");
            sb.append(portEventCounts[15]);
            sb.append(" , ");
            for (int i = 10; i < 16; i++) {
                portEventCounts[i] = 0;
            }
        }
    }

    public boolean clearWaterCounter(int machineNum) {
        ModbusTCPTransaction trans = new ModbusTCPTransaction(mConnection);
        WriteMultipleCoilsRequest req = null;
        if (machineNum == 1) {
            req = new WriteMultipleCoilsRequest(272, 2);
        } else if (machineNum == 2) {
            req = new WriteMultipleCoilsRequest(280, 2);
        }
        if (req != null) {
            req.setCoilStatus(0, true);
            req.setCoilStatus(1, true);
        }
//        req.setUnitID(1);
        trans.setRequest(req);
        try {
            trans.execute();
        } catch (ModbusException e) {
            e.printStackTrace();
        }
        WriteMultipleCoilsResponse response = (WriteMultipleCoilsResponse) trans.getResponse();
        boolean result = response.getBitCount() == 1;
        return result;

    }

    public int[] getMachine1WaterReadings() {
        ModbusTransaction trans = new ModbusTCPTransaction(mConnection);
        ReadInputRegistersRequest req = new ReadInputRegistersRequest(16,4);

        trans.setRequest(req);

        try {
            trans.execute();
        } catch (ModbusException e) {
            e.printStackTrace();
        }
        ReadInputRegistersResponse response = (ReadInputRegistersResponse) trans.getResponse();
        return new int[]{response.getRegisterValue(1), response.getRegisterValue(3)};
    }

    public int[] getMachine2WaterReadings() {
        ModbusTransaction trans = new ModbusTCPTransaction(mConnection);
        ReadInputRegistersRequest req = new ReadInputRegistersRequest(32,4);

        trans.setRequest(req);

        try {
            trans.execute();
        } catch (ModbusException e) {
            e.printStackTrace();
        }
        ReadInputRegistersResponse response = (ReadInputRegistersResponse) trans.getResponse();
        return new int[]{response.getRegisterValue(1), response.getRegisterValue(3)};

    }

    private String getWaterReadings() {
        ModbusTransaction trans = new ModbusTCPTransaction(mConnection);
        ReadInputRegistersRequest req = new ReadInputRegistersRequest(16,32);

        trans.setRequest(req);

        try {
            trans.execute();
        } catch (ModbusException e) {
            e.printStackTrace();
        }
        ReadInputRegistersResponse response = (ReadInputRegistersResponse) trans.getResponse();
        StringBuilder sb = new StringBuilder();
        int k = 0;
        for (int i = 1; i < mCount; i+=2) {

            sb.append("Water Meter #: ");
            sb.append(k++);
            sb.append(" Value: ");
            sb.append(response.getRegisterValue(i));
            sb.append("\n");
        }
        return sb.toString();
    }

    private void writeEventToFile(PortEvent event) {

        if (mOut == null) {
            try {
                mOut = new PrintWriter(new BufferedWriter(new FileWriter(mFile, true)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String type = event.getType().name();
        String portNum = String.valueOf(event.getPortNum());
        String timestamp = event.getTimestamp().toString();
        String duration = String.valueOf(event.getDuration());
        String output = type + " - " + "Port Number: " + portNum + " - Event Time: " + timestamp + " - Duration: " + duration;

        mOut.println(output);
        mOut.close();
        mOut = null;
    }

    private void writeWaterReadingsToFile(String readings) {
        if (mOut == null) {
            try {
                mOut = new PrintWriter(new BufferedWriter(new FileWriter(mFile, true)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mOut.println(readings);
        mOut.close();
        mOut = null;
    }

    public FileLogWriter getMachine1WaterLogWriter() {
        return mMachine1WaterLogWriter;
    }

    public void setMachine1WaterLogWriter(FileLogWriter machine1WaterLogWriter) {
        mMachine1WaterLogWriter = machine1WaterLogWriter;
    }

    public FileLogWriter getMachine2WaterLogWriter() {
        return mMachine2WaterLogWriter;
    }

    public void setMachine2WaterLogWriter(FileLogWriter machine2WaterLogWriter) {
        mMachine2WaterLogWriter = machine2WaterLogWriter;
    }
}
