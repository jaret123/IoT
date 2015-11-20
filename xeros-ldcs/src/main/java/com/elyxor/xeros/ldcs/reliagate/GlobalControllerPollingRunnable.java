package com.elyxor.xeros.ldcs.reliagate;

import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.net.TCPMasterConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by will on 9/16/15.
 */
public class GlobalControllerPollingRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GlobalControllerPollingRunnable.class);
    private static final String TAG = "PollingRunnable";
    private PollingResultListener mListener;
    private TCPMasterConnection mConnection;
    private boolean mIsMock;

    private int mRef = 10;
    private int mCount = 2;

    private int mRef2 = 18;
    private int mCount2 = 13;

    private int mRef3 = 32;
    private int mCount3 = 28;

    private int mRef4 = 70;
    private int mCount4 = 7;

    private List<GlobalControllerModbusPort> mCoilList;
    private List<GlobalControllerModbusPort> mRegisterList;

    private Map<Integer, Integer> mPreviousCoilStatus;
    private Map<Integer, Integer> mPreviousRegisterStatus;

    public GlobalControllerPollingRunnable(PollingResultListener listener, TCPMasterConnection connection, boolean isMock) {
        this.mListener = listener;
        if (!isMock) {
            this.mConnection = new TCPMasterConnection(connection.getAddress());
            this.mConnection.setPort(connection.getPort());

            try {
                this.mConnection.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.mIsMock = isMock;

        this.mCoilList = GlobalControllerPortMap.mCoilMap;
        this.mRegisterList = GlobalControllerPortMap.mRegisterMap;

        this.mPreviousCoilStatus = new HashMap<Integer, Integer>(mCoilList.size());
        this.mPreviousRegisterStatus = new HashMap<Integer, Integer>(mRegisterList.size());
    }
    @Override public void run() {

//        while (mIsMock) {
//            try {
//                mListener.onPortChanged(58, 1);
//                Thread.sleep(1000);
//                mListener.onPortChanged(58, 0);
//                for (Map.Entry<Integer, String> entry : GlobalControllerPortMap.mCoilMap.entrySet()) {
//                    mListener.onPortChanged(entry.getKey(), 1);
//                    Thread.sleep(3000);
//                    mListener.onPortChanged(entry.getKey(), 0);
//                }
//                for (Map.Entry<Integer, String> entry : GlobalControllerPortMap.mRegisterMap.entrySet()) {
//                    mListener.onRegisterChanged(entry.getKey(), (int) (Math.random() * 50));
//                    Thread.sleep(5000);
//                    mListener.onRegisterChanged(entry.getKey(), (int) (Math.random() * 50));
//                }
//
//                Thread.sleep(2000);
//            } catch (Exception e) {
//                logger.warn(TAG, "Interrupted Exception");
//            }
//        }


        while (true) {
            for (GlobalControllerModbusPort port : mCoilList) {
                int key = port.getPortAddress();
                ModbusTCPTransaction trans = new ModbusTCPTransaction(mConnection);
                ReadCoilsRequest request = new ReadCoilsRequest(key, 1);
                trans.setRequest(request);
                try {
                    trans.execute();
                } catch (Exception e) {
                    logger.warn(TAG, e.getMessage() != null ? e.getMessage() : "");
                }
                ModbusResponse response = trans.getResponse();
                if (response instanceof ExceptionResponse) {
                    logger.warn(TAG, ((ExceptionResponse)response).getHexMessage());
                }
                else {

                    //is the port currently "on"
                    boolean coilStatus = ((ReadCoilsResponse) response).getCoilStatus(0);

                    //is the port normally "off"
                    boolean portRestingFalse = port.getOffValue() == 0;

                    //if currently off, and at resting is off, set to off state. If currently on, and at resting is off, set to on state.
                    //if currently on, and at resting is on, set to off state. If currently off, and at resting is on, set to on state.
                    int value = coilStatus == portRestingFalse ? 0 : 1;

//                    int value = ((ReadCoilsResponse) response).getCoilStatus(0) ? 1 : 0;
                    if (mPreviousCoilStatus.get(key) == null) {
                        logger.info("Response Value was null, inserting key: "+key + " and value: " + value);

                        mPreviousCoilStatus.put(key, value);
                    }
                    else if (value != mPreviousCoilStatus.get(key)) {
                        logger.info("Response Value has changed, posting onPortChanged key: "+key + " and value: " + value);

                        mListener.onPortChanged(key, value);
                        mPreviousCoilStatus.put(key, value);
                    } else {
                        mPreviousCoilStatus.put(key, value);
                    }
                }
            }

            for (GlobalControllerModbusPort port : mRegisterList) {
                int key = port.getPortAddress();
                ModbusTCPTransaction trans = new ModbusTCPTransaction(mConnection);
                ReadInputRegistersRequest request = new ReadInputRegistersRequest(key, 1);
                trans.setRequest(request);
                try {
                    trans.execute();
                } catch (Exception e) {
                    logger.warn(TAG, e.getMessage() != null ? e.getMessage() : "");
                }
                ModbusResponse response = trans.getResponse();
                if (response instanceof ExceptionResponse) {
                    logger.warn(TAG, ((ExceptionResponse)response).getHexMessage());
                }
                else {
                    int value = ((ReadInputRegistersResponse) response).getRegisterValue(0);
                    if (mPreviousRegisterStatus.get(key) == null) {
                        mPreviousRegisterStatus.put(key, value);
                    }
                    else if (value != mPreviousRegisterStatus.get(key)) {
                        mListener.onRegisterChanged(key, value);
                        mPreviousRegisterStatus.put(key, value);
                    } else {
                        mPreviousRegisterStatus.put(key, value);
                    }
                }

            }

        }
    }
}


//if (mIsMock) {
//        try {
//        mListener.onPortChanged(2, 1);
//        mListener.onPortChanged(3, 1);
//        Thread.sleep(5300);
//        mListener.onPortChanged(3, 0);
//        Thread.sleep(1000);
//        mListener.onPortChanged(4, 1);
//        Thread.sleep(2000);
//        mListener.onPortChanged(4, 0);
//        mListener.onPortChanged(2, 0);
//        } catch (InterruptedException e) {
//        e.printStackTrace();
//        }
//        }
//
//        mTransaction = new ModbusTCPTransaction(mConnection);
//        mReadRequest = new ReadCoilsRequest(mRef, mCount);
//        mReadRequest.setUnitID(2);
//        mTransaction.setRequest(mReadRequest);
//
//        mTransaction2 = new ModbusTCPTransaction(mConnection);
//        mReadRequest2 = new ReadCoilsRequest(mRef2, mCount2);
//        mReadRequest2.setUnitID(2);
//        mTransaction2.setRequest(mReadRequest2);
//
//        mTransaction3 = new ModbusTCPTransaction(mConnection);
//        mReadRequest3 = new ReadCoilsRequest(mRef3, mCount3);
//        mReadRequest3.setUnitID(2);
//        mTransaction3.setRequest(mReadRequest3);
//
//        mTransaction4 = new ModbusTCPTransaction(mConnection);
//        mReadRequest4 = new ReadCoilsRequest(mRef4, mCount4);
//        mReadRequest4.setUnitID(2);
//        mTransaction4.setRequest(mReadRequest4);

//            try {
//                mTransaction.execute();
//                mTransaction2.execute();
//                mTransaction3.execute();
//                mTransaction4.execute();
//            } catch (ModbusException e) {
//                e.printStackTrace();
//            }
//            ModbusResponse response = mTransaction.getResponse();
//            ModbusResponse response2 = mTransaction2.getResponse();
//            ModbusResponse response3 = mTransaction3.getResponse();
//            ModbusResponse response4 = mTransaction4.getResponse();
//
//            if (response instanceof ExceptionResponse) {
//                logger.warn(TAG, ((ExceptionResponse)response).getHexMessage());
//            }
//            else {
//                mReadResponse = (ReadCoilsResponse) mTransaction.getResponse();
//                for (int i = 0; i < mCount; i++) {
//                    int value = mReadResponse.getCoilStatus(i) ? 1 : 0;
//                    if (value != mPreviousPortStatus[i]) {
//                        mListener.onPortChanged(i, value);
//                    }
//                    mPreviousPortStatus[i] = value;
//                }
//            }
//            if (response2 instanceof ExceptionResponse) {
//                logger.warn(TAG, ((ExceptionResponse)response2).getHexMessage());
//            }
//            else {
//                mReadResponse2 = (ReadCoilsResponse) mTransaction2.getResponse();
//                for (int i = 0; i < mCount2; i++) {
//                    int value = mReadResponse2.getCoilStatus(i) ? 1 : 0;
//                    if (value != mPreviousPortStatus2[i]) {
//                        mListener.onPortChanged(i, value);
//                    }
//                    mPreviousPortStatus2[i] = value;
//                }
//            }
//            if (response3 instanceof ExceptionResponse) {
//                logger.warn(TAG, ((ExceptionResponse)response3).getHexMessage());
//            }
//            else {
//                mReadResponse3 = (ReadCoilsResponse) mTransaction3.getResponse();
//                for (int i = 0; i < mCount3; i++) {
//                    int value = mReadResponse3.getCoilStatus(i) ? 1 : 0;
//                    if (value != mPreviousPortStatus3[i]) {
//                        mListener.onPortChanged(i, value);
//                    }
//                    mPreviousPortStatus3[i] = value;
//                }
//            }
//            if (response4 instanceof ExceptionResponse) {
//                logger.warn(TAG, ((ExceptionResponse)response4).getHexMessage());
//            }
//            else {
//                mReadResponse4 = (ReadCoilsResponse) mTransaction4.getResponse();
//                for (int i = 0; i < mCount4; i++) {
//                    int value = mReadResponse4.getCoilStatus(i) ? 1 : 0;
//                    if (value != mPreviousPortStatus4[i]) {
//                        mListener.onPortChanged(i, value);
//                    }
//                    mPreviousPortStatus4[i] = value;
//                }
//            }
