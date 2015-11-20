package com.elyxor.xeros.ldcs.reliagate;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by will on 11/12/15.
 */
public class GlobalControllerPortMap {
    public static final Map<Integer, String> mCoilMap;
    public static final Map<Integer, String> mRegisterMap;

    static {
        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(10, "SystemEnabled");
        map.put(11, "BwDoseDone");
        map.put(18, "ColdWaterFillStep");
        map.put(19, "HotWaterFillStep");
        map.put(20, "BwDosingStep");
        map.put(21, "CupDosingStep");
        map.put(22, "TankToSumpStep");
        map.put(23, "MachineHeatStep");
        map.put(24, "TankHeatStep");
        map.put(25, "BeadsStep");
        map.put(26, "DrainStep");
        map.put(27, "TumbleStep");
        map.put(28, "ExtractStep");
        map.put(29, "TankSprayStep");
        map.put(30, "SumpSprayStep");
        map.put(32, "BwDosingEnabled");
        map.put(33, "EndOfWashProgramStep");
        map.put(34, "DrumRotationFault");
        map.put(35, "ExternalDosingFault");
        map.put(36, "SumpLevelOverflow");
        map.put(37, "BeadPulseTimeout");
        map.put(38, "SumpLevelOutOfRange");
        map.put(39, "TankLevelOutOfRange");
        map.put(40, "TankTempOutOfRange");
        map.put(41, "TankEmpty");
        map.put(42, "SumpWaterFillEvent");
        map.put(43, "ExtraWaterAddedEvent");
        map.put(44, "OutOfBalanceWarningAlarm");
        map.put(45, "OutOfBalanceAlarm");
        map.put(46, "DrumDoorNotClosedAlarm");
        map.put(47, "TankHeatingAlarm");
        map.put(48, "BeadPumpOLAlarm");
        map.put(49, "WaterPumpOLAlarm");
        map.put(50, "PlcBatteryAlarm");
        map.put(51, "InverterFailedToStartAlarm");
        map.put(52, "TankNoFillAlarm");
        map.put(53, "SumpNoFillAlarm");
        map.put(54, "BwFail");
        map.put(55, "SumpTempOutOfRangeAlarm");
        map.put(56, "SumpTopUpActiveEvent");
        map.put(57, "SumpRefillActiveEvent");
        map.put(58, "EStopPushedAlarm");
        map.put(59, "InverterFaultAlarm");
        map.put(70, "CycleStart");
        map.put(71, "CycleStop");
        map.put(72, "CyclePause");
        map.put(73, "BwStart");
        map.put(74, "DoseCup");
        map.put(75, "DrumMotorOk");
        map.put(76, "DrumDoorLocked");
        mCoilMap = map;

        HashMap<Integer, String> registerMap = new HashMap<Integer, String>();
        registerMap.put(100, "TankLevel");
        registerMap.put(110, "TankTemp");
        registerMap.put(120, "SumpLevel");
        registerMap.put(130, "SumpTemp");
        registerMap.put(140, "BwProgramNo");
        registerMap.put(150, "DrumGs");
        registerMap.put(160, "CurrentProgTime");
        registerMap.put(170, "AdjustedProgTime");
        mRegisterMap = registerMap;
    }



}
