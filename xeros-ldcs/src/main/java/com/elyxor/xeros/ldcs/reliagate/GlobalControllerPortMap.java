package com.elyxor.xeros.ldcs.reliagate;

import com.elyxor.xeros.ldcs.AppConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by will on 11/12/15.
 */
public class GlobalControllerPortMap {
    public static List<GlobalControllerModbusPort> mCoilMap = new ArrayList<GlobalControllerModbusPort>(500);
    public static List<GlobalControllerModbusPort> mRegisterMap = new ArrayList<GlobalControllerModbusPort>(500);

    public GlobalControllerPortMap() {
        List<String> portList = AppConfiguration.getGlobalControllerPortList();
        for (String s : portList) {
            GlobalControllerModbusPort port = new GlobalControllerModbusPort();
            String[] equalSplit = s.split("=");

            port.setPortAddress(Integer.parseInt(equalSplit[0]));

            String[] commaSplit = equalSplit[1].split(",");
            port.setPortName(commaSplit[0]);
            port.setOffValue(Integer.parseInt(commaSplit[1]));

            if (port.getPortAddress() < 100) {
                mCoilMap.add(port);
            } else {
                mRegisterMap.add(port);
            }
        }
    }

    public List<GlobalControllerModbusPort> getCoilMap() {
        return mCoilMap;
    }

    public List<GlobalControllerModbusPort> getRegisterMap() {
        return mRegisterMap;
    }

    public int getMaxPortNum() {
        int max = 0;
        for (GlobalControllerModbusPort port : mCoilMap) {
            if (port.getPortAddress() > max) {
                max = port.getPortAddress();
            }
        }
        for (GlobalControllerModbusPort port : mRegisterMap) {
            if (port.getPortAddress() > max) {
                max = port.getPortAddress();
            }
        }
        return max;
    }
//    static {
//        List<GlobalControllerModbusPort> map = new ArrayList<GlobalControllerModbusPort>();
//        map.add(new GlobalControllerModbusPort(10, "SystemEnabled", 0));
//        map.add(new GlobalControllerModbusPort(11, "BwDoseDone", 0));
//        map.add(new GlobalControllerModbusPort(18, "ColdWaterFillStep", 0));
//        map.add(new GlobalControllerModbusPort(19, "HotWaterFillStep", 0));
//        map.add(new GlobalControllerModbusPort(20, "BwDosingStep", 0));
//        map.add(new GlobalControllerModbusPort(21, "CupDosingStep", 0));
//        map.add(new GlobalControllerModbusPort(22, "TankToSumpStep", 0));
//        map.add(new GlobalControllerModbusPort(23, "MachineHeatStep", 0));
//        map.add(new GlobalControllerModbusPort(24, "TankHeatStep", 0));
//        map.add(new GlobalControllerModbusPort(25, "BeadsStep", 0));
//        map.add(new GlobalControllerModbusPort(26, "DrainStep", 0));
//        map.add(new GlobalControllerModbusPort(27, "TumbleStep", 0));
//        map.add(new GlobalControllerModbusPort(28, "ExtractStep", 0));
//        map.add(new GlobalControllerModbusPort(29, "TankSprayStep", 0));
//        map.add(new GlobalControllerModbusPort(30, "SumpSprayStep", 0));
//        map.add(new GlobalControllerModbusPort(32, "BwDosingEnabled", 0));
//        map.add(new GlobalControllerModbusPort(33, "EndOfWashProgramStep", 0));
//        map.add(new GlobalControllerModbusPort(34, "DrumRotationFault", 0));
//        map.add(new GlobalControllerModbusPort(35, "ExternalDosingFault", 0));
//        map.add(new GlobalControllerModbusPort(36, "SumpLevelOverflow", 0));
//        map.add(new GlobalControllerModbusPort(37, "BeadPulseTimeout", 0));
//        map.add(new GlobalControllerModbusPort(38, "SumpLevelOutOfRange", 0));
//        map.add(new GlobalControllerModbusPort(39, "TankLevelOutOfRange", 0));
//        map.add(new GlobalControllerModbusPort(40, "TankTempOutOfRange", 0));
//        map.add(new GlobalControllerModbusPort(41, "TankEmpty", 0));
//        map.add(new GlobalControllerModbusPort(42, "SumpWaterFillEvent", 0));
//        map.add(new GlobalControllerModbusPort(43, "ExtraWaterAddedEvent", 0));
//        map.add(new GlobalControllerModbusPort(44, "OutOfBalanceWarningAlarm", 0));
//        map.add(new GlobalControllerModbusPort(45, "OutOfBalanceAlarm", 0));
//        map.add(new GlobalControllerModbusPort(46, "DrumDoorNotClosedAlarm", 0));
//        map.add(new GlobalControllerModbusPort(47, "TankHeatingAlarm", 0));
//        map.add(new GlobalControllerModbusPort(48, "BeadPumpOLAlarm", 0));
//        map.add(new GlobalControllerModbusPort(49, "WaterPumpOLAlarm", 0));
//        map.add(new GlobalControllerModbusPort(50, "PlcBatteryAlarm", 0));
//        map.add(new GlobalControllerModbusPort(51, "InverterFailedToStartAlarm", 0));
//        map.add(new GlobalControllerModbusPort(52, "TankNoFillAlarm", 0));
//        map.add(new GlobalControllerModbusPort(53, "SumpNoFillAlarm", 0));
//        map.add(new GlobalControllerModbusPort(54, "BwFail", 0));
//        map.add(new GlobalControllerModbusPort(55, "SumpTempOutOfRangeAlarm", 0));
//        map.add(new GlobalControllerModbusPort(56, "SumpTopUpActiveEvent", 0));
//        map.add(new GlobalControllerModbusPort(57, "SumpRefillActiveEvent", 0));
//        map.add(new GlobalControllerModbusPort(58, "EStopPushedAlarm", 0));
//        map.add(new GlobalControllerModbusPort(59, "InverterFaultAlarm", 0));
//        map.add(new GlobalControllerModbusPort(70, "CycleStart", 1));
//        map.add(new GlobalControllerModbusPort(71, "CycleStop", 0));
//        map.add(new GlobalControllerModbusPort(72, "CyclePause", 0));
//        map.add(new GlobalControllerModbusPort(73, "BwStart", 0));
//        map.add(new GlobalControllerModbusPort(74, "DoseCup", 1));
//        map.add(new GlobalControllerModbusPort(75, "DrumMotorOk", 0));
//        map.add(new GlobalControllerModbusPort(76, "DrumDoorLocked", 0));
//        mCoilMap = map;
//
//        List<GlobalControllerModbusPort> registerMap = new ArrayList<GlobalControllerModbusPort>();
//        registerMap.add(new GlobalControllerModbusPort(100, "TankLevel", 0));
//        registerMap.add(new GlobalControllerModbusPort(110, "TankTemp", 0));
//        registerMap.add(new GlobalControllerModbusPort(120, "SumpLevel", 0));
//        registerMap.add(new GlobalControllerModbusPort(130, "SumpTemp", 0));
//        registerMap.add(new GlobalControllerModbusPort(140, "BwProgramNo", 0));
//        registerMap.add(new GlobalControllerModbusPort(150, "DrumGs", 0));
//        registerMap.add(new GlobalControllerModbusPort(160, "CurrentProgTime", 0));
//        registerMap.add(new GlobalControllerModbusPort(170, "AdjustedProgTime", 0));
//        mRegisterMap = registerMap;
//    }
//
    public GlobalControllerModbusPort findPort(int portAddress) {
        GlobalControllerModbusPort result = new GlobalControllerModbusPort();
        List<GlobalControllerModbusPort> portList = new ArrayList<GlobalControllerModbusPort>();

        portList.addAll(mCoilMap);
        portList.addAll(mRegisterMap);

        for (GlobalControllerModbusPort port : portList) {
            if (port.getPortAddress() == portAddress) {
                result =  port;
            }
        }
        return result;
    }
}
