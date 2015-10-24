package com.elyxor.xeros.ldcs.reliagate;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;

/**
 * Created by will on 10/2/15.
 */
public class WaterOnlyPollingRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(WaterOnlyPollingRunnable.class);
    private ReliagatePort mPort;
    private int[] machine1MeterDiff = new int[2];
    private int[] machine2MeterDiff = new int[2];
    private boolean machine1LogSent;
    private boolean machine2LogSent;
    private int mWaterOnly;

    private boolean isRunning;

    public WaterOnlyPollingRunnable(ReliagatePort mPort, int waterOnly) {
        this.mPort = mPort;
        this.mWaterOnly = waterOnly;
        if (mWaterOnly == 1 || mWaterOnly == 3) {
            initMachine2Water();
        }
        if (mWaterOnly == 3) {
            initMachine1Water();
        }
    }

    @Override public void run() {
        isRunning = true;

        while (isRunning) {
            if (mWaterOnly == 1 || mWaterOnly == 3) {
                calculateMachine2Readings();
            }
            if (mWaterOnly == 3) {
                calculateMachine1Readings();
            }
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String initMachine1Water() {
        int[] meters = parseMachine1PrevReadingsFromFile();
        machine1LogSent = true;
        if (Arrays.equals(meters, new int[]{0, 0})) {
            machine1LogSent = false;
            meters = calculateMachine1Readings();
        }
        return Arrays.toString(meters);
    }

    private String initMachine2Water() {
        int[] meters = parseMachine2PrevReadingsFromFile();
        machine2LogSent = true;
        if (Arrays.equals(meters, new int[]{0, 0})) {
            machine2LogSent = false;
            meters = calculateMachine2Readings();
        }
        return Arrays.toString(meters);
    }


    private int[] calculateMachine1Readings() {
        int[] result = null;
        int[] current = mPort.getMachine1WaterReadings();
        int[] prev = parseMachine1PrevReadingsFromFile();

        if (current[0] == 0 && current[1] == 0) {
            logger.warn("no machine 1 water meter readings found");
            return result;
        }
        logger.info("Captured machine 1 water readings, meter1: "+current[0]
                + ", meter2: "+current[1]);
        int meter1 = current[0] - prev[0];
        int meter2 = current[1] - prev[1];

        logger.info("Machine 1 Water Diff: meter1: "+meter1+", meter2: "+meter2);

        if (meter1 == 0 && meter2 == 0){
            if (!machine1LogSent) {
                result = new int[2];
                result[0] = machine1MeterDiff[0];
                result[1] = machine1MeterDiff[1];
                machine1MeterDiff[0] = machine1MeterDiff[1] = 0;
                machine1LogSent = true;

                current = new int[]{0,0};
                mPort.clearWaterCounter(1);

                try {
                    Files.delete(mPort.getMachine1WaterLogWriter().getFile().toPath());
                } catch (IOException e) {
                    String msg = "Machine 1 Water Meter Log File Not Found";
                    logger.warn(msg, e);
                }
                writeMachine1WaterLog(result);
                storeMachine1PrevMeters(current);

                return result;
            }
            return result;
        }
        machine1MeterDiff[0] += meter1;
        machine1MeterDiff[1] += meter2;
        try {
            Files.delete(mPort.getMachine1WaterLogWriter().getFile().toPath());
        } catch (IOException e) {
            String msg = "Machine 1 Water Meter Log File Not Found";
            logger.warn(msg, e);
        }
        machine1LogSent = false;
        storeMachine1PrevMeters(current);
        return result;
    }

    private int[] calculateMachine2Readings() {
        int[] result = null;
        int[] current = mPort.getMachine2WaterReadings();
        int[] prev = parseMachine2PrevReadingsFromFile();

        if (current[0] == 0 && current[1] == 0) {
            logger.warn("no machine 2 water meter readings found");
            return result;
        }
        logger.info("Captured machine 2 water readings, meter1: "+current[0]
                + ", meter2: "+current[1]);
        int meter1 = current[0] - prev[0];
        int meter2 = current[1] - prev[1];

        logger.info("Machine 2 Water Diff: meter1: "+meter1+", meter2: "+meter2);

        if (meter1 == 0 && meter2 == 0){
            if (!machine2LogSent) {
                result = new int[2];
                result[0] = machine2MeterDiff[0];
                result[1] = machine2MeterDiff[1];
                machine2MeterDiff[0] = machine2MeterDiff[1] = 0;
                machine2LogSent = true;

                current = new int[]{0,0};
                mPort.clearWaterCounter(2);

                try {
                    Files.delete(mPort.getMachine2WaterLogWriter().getFile().toPath());
                } catch (IOException e) {
                    String msg = "Machine 1 Water Meter Log File Not Found";
                    logger.warn(msg, e);
                }

                writeMachine2WaterLog(result);
                storeMachine2PrevMeters(current);

                return result;
            }
            return result;
        }
        machine2MeterDiff[0] += meter1;
        machine2MeterDiff[1] += meter2;
        try {
            Files.delete(mPort.getMachine2WaterLogWriter().getFile().toPath());
        } catch (IOException e) {
            String msg = "Machine 2 Water Meter Log File Not Found";
            logger.warn(msg, e);
        }
        machine2LogSent = false;
        storeMachine2PrevMeters(current);
        return result;

    }

    private void storeMachine1PrevMeters(int[] meters) {
        try {
            mPort.getMachine1WaterLogWriter().write("meter1,"+meters[0]+","+getSystemTimeAndDate()+"\n");
            logger.info("Successfully Stored Machine 1 prev meter1: "+meters[0]);
            mPort.getMachine1WaterLogWriter().write("meter2,"+meters[1]+","+getSystemTimeAndDate()+"\n");
            logger.info("Successfully Stored Machine 1 prev meter2: "+meters[1]);

        } catch (IOException e) {
            String msg = "Failed to Store Prev Meters";
            logger.warn(msg, e);
        }
    }

    private void storeMachine2PrevMeters(int[] meters) {
        try {
            mPort.getMachine2WaterLogWriter().write("meter1,"+meters[0]+","+getSystemTimeAndDate()+"\n");
            logger.info("Successfully Stored Machine 2 prev meter1: "+meters[0]);
            mPort.getMachine2WaterLogWriter().write("meter2,"+meters[1]+","+getSystemTimeAndDate()+"\n");
            logger.info("Successfully Stored Machine 2 prev meter2: "+meters[1]);

        } catch (IOException e) {
            String msg = "Failed to Store Prev Meters";
            logger.warn(msg, e);
        }

    }

    private int[] parseMachine1PrevReadingsFromFile() {
        byte[] inputData = null;
        try {
            inputData = IOUtils.toByteArray(new FileReader(mPort.getMachine1WaterLogWriter().getFile()));
        } catch (Exception ex) {
            logger.warn("could not open meter log file",ex);
            return new int[]{0,0};
        }

        StringBuffer fString = new StringBuffer();
        for ( byte b : inputData ){
            if( (int)b<10 ) {
                continue;
            }
            fString.append((char)b);
        }
        String[] lines = fString.toString().split("\n");
        int[] prev = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            if (lines[i]!=null) {
                String[] lineData = lines[i].split(",");
                prev[i] = Integer.parseInt(lineData[1]);
            }
        }
        return prev;
    }
    private int[] parseMachine2PrevReadingsFromFile() {
        byte[] inputData = null;
        try {
            inputData = IOUtils.toByteArray(new FileReader(mPort.getMachine2WaterLogWriter().getFile()));
        } catch (Exception ex) {
            logger.warn("could not open meter log file",ex);
            return new int[]{0,0};
        }

        StringBuffer fString = new StringBuffer();
        for ( byte b : inputData ){
            if( (int)b<10 ) {
                continue;
            }
            fString.append((char)b);
        }
        String[] lines = fString.toString().split("\n");
        int[] prev = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            if (lines[i]!=null) {
                String[] lineData = lines[i].split(",");
                prev[i] = Integer.parseInt(lineData[1]);
            }
        }
        return prev;
    }

    public void writeMachine1WaterLog(int[] meters) {
        try {
            mPort.getWriter().write(mPort.getDaiPrefix() + ", Xeros , \nFile Write Time: , "
                    + getSystemTimeAndDate() + "\n"
                    + "WM2: , 0 , 0 , "
                    + meters[0] + "\n"
                    + "WM3: , 0 , 0 , "
                    + meters[1]);
        } catch (IOException e) {
            logger.warn("failed to write " + Arrays.toString(meters) + "to log file.");
        }
        logger.info("wrote water meter log to file");
    }

    public void writeMachine2WaterLog(int[] meters) {
        try {
            mPort.getWriter().write(mPort.getDaiPrefix() + ", Std , \nFile Write Time: , "
                    + getSystemTimeAndDate() + "\n"
                    + "WM2: , 0 , 0 , "
                    + meters[0] + "\n"
                    + "WM3: , 0 , 0 , "
                    + meters[1]);
        } catch (IOException e) {
            logger.warn("failed to write " + Arrays.toString(meters) + "to log file.");
        }
        logger.info("wrote water meter log to file");
    }

    private String getSystemTimeAndDate() {
        String result = "";
        SimpleDateFormat timingFormat = new SimpleDateFormat("dd-MM-yyyy kk : mm : ss");
        result = timingFormat.format(System.currentTimeMillis());
        return result;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }
}
