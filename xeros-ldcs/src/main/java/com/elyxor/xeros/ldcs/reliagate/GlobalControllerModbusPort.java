package com.elyxor.xeros.ldcs.reliagate;

/**
 * Created by will on 11/12/15.
 */
public class GlobalControllerModbusPort {
    private int mPortAddress;
    private String mPortName;
    private int mOffValue;

    public GlobalControllerModbusPort() {}

    //offValue is 0 or 1. 0 means, this port is Off when the reading is 0. 1 means, this port is ON when reading 0.
    public GlobalControllerModbusPort(int portAddress, String portName, int offValue) {
        this.mPortAddress = portAddress;
        this.mPortName = portName;
        this.mOffValue = offValue;
    }

    public int getPortAddress() {
        return mPortAddress;
    }

    public void setPortAddress(int portAddress) {
        mPortAddress = portAddress;
    }

    public String getPortName() {
        return mPortName;
    }

    public void setPortName(String portName) {
        mPortName = portName;
    }

    public int getOffValue() {
        return mOffValue;
    }

    public void setOffValue(int offValue) {
        mOffValue = offValue;
    }
}
