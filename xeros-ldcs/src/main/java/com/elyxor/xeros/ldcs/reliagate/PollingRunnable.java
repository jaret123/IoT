package com.elyxor.xeros.ldcs.reliagate;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ExceptionResponse;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.net.TCPMasterConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by will on 9/16/15.
 */
public class PollingRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PollingRunnable.class);
    private static final String TAG = "PollingRunnable";
    private PollingResultListener mListener;
    private TCPMasterConnection mConnection;
    private boolean mIsMock;


    private ModbusTCPTransaction mTransaction;
    private ReadInputDiscretesRequest mReadRequest;
    private ReadInputDiscretesResponse mReadResponse;

    private ModbusTCPTransaction mMachine1Tran;
    private ReadInputDiscretesRequest mMachine1Request;
    private ReadInputDiscretesResponse mMachine1Response;

    private ModbusTCPTransaction mMachine2Tran;
    private ReadInputDiscretesRequest mMachine2Request;
    private ReadInputDiscretesResponse mMachine2Response;

    private int mRef = 0;
    private int mCount = 16;

    private int machine1Ref = 2;
    private int machine1Count = 6;

    private int machine2Ref = 10;
    private int machine2Count = 6;

    private int[] mPreviousPortStatus = new int[mCount];

    private int[] mMachine1PrevPortStatus = new int[machine1Count];
    private int[] mMachine2PrevPortStatus = new int[machine2Count];

    public PollingRunnable(PollingResultListener listener, TCPMasterConnection connection, boolean isMock) {
        this.mListener = listener;
        this.mConnection = connection;
//        this.mConnection.setPort(connection.getPort());
//        try {
//            this.mConnection.connect();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        this.mIsMock = isMock;
    }
    @Override public void run() {
        if (mIsMock) {
            try {
                mListener.onPortChanged(2, 1);
                mListener.onPortChanged(3, 1);
                Thread.sleep(5300);
                mListener.onPortChanged(3, 0);
                Thread.sleep(1000);
                mListener.onPortChanged(4, 1);
                Thread.sleep(2000);
                mListener.onPortChanged(4, 0);
                mListener.onPortChanged(2, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mMachine1Tran = new ModbusTCPTransaction(mConnection);
        mMachine1Request = new ReadInputDiscretesRequest(machine1Ref, machine1Count);
        mMachine1Tran.setRequest(mMachine1Request);

        mMachine2Tran = new ModbusTCPTransaction(mConnection);
        mMachine2Request = new ReadInputDiscretesRequest(machine2Ref, machine2Count);
        mMachine2Tran.setRequest(mMachine2Request);

        while (true) {
            try {
                mMachine1Tran.execute();
            } catch (ModbusException e) {
                e.printStackTrace();
            }
            logger.debug(TAG, "");
            ModbusResponse machine1Response = mMachine1Tran.getResponse();

            if (machine1Response instanceof ExceptionResponse) {
                logger.warn(TAG, ((ExceptionResponse)machine1Response).getHexMessage());
            }
            else {
                try {
                    mMachine1Response = (ReadInputDiscretesResponse) mMachine1Tran.getResponse();
                } catch (Exception e) {
                    mMachine1Response = null;
                }
                if (mMachine1Response != null) {
                    for (int i = 0; i < machine1Count; i++) {

                        int value = mMachine1Response.getDiscreteStatus(i) ? 1 : 0;
                        if (value != mMachine1PrevPortStatus[i]) {
                            mListener.onPortChanged(i + machine1Ref, value);
                        }
                        mMachine1PrevPortStatus[i] = value;
                    }
                }
            }
            try {
                Thread.sleep(100);
                mMachine2Tran.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.debug(TAG, "");
            ModbusResponse machine2Response = mMachine2Tran.getResponse();

            if (machine2Response instanceof ExceptionResponse) {
                logger.warn(TAG, ((ExceptionResponse)machine2Response).getHexMessage());
            }
            else {
                try {
                    mMachine2Response = (ReadInputDiscretesResponse) mMachine2Tran.getResponse();
                } catch (Exception e) {
                    mMachine2Response = null;
                }
                if (mMachine2Response != null) {
                    for (int i = 0; i < machine2Count; i++) {

                        int value = mMachine2Response.getDiscreteStatus(i) ? 1 : 0;
                        if (value != mMachine2PrevPortStatus[i]) {
                            mListener.onPortChanged(i + machine2Ref, value);
                        }
                        mMachine2PrevPortStatus[i] = value;
                    }
                }
            }
            try {
                Thread.sleep(100);
                mMachine2Tran.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }









            if (machine2Response instanceof ExceptionResponse) {
                logger.warn(TAG, ((ExceptionResponse)machine2Response).getHexMessage());
            }
            else {
                try {
                    mMachine2Response = (ReadInputDiscretesResponse) mMachine2Tran.getResponse();
                } catch (Exception e) {
                    mMachine2Response = null;
                }
                if (mMachine2Response != null) {
                    for (int i = 0; i < machine2Count; i++) {

                        int value = mMachine2Response.getDiscreteStatus(i) ? 1 : 0;
                        if (value != mMachine2PrevPortStatus[i]) {
                            mListener.onPortChanged(i + machine2Ref, value);
                        }
                        mMachine2PrevPortStatus[i] = value;
                    }
                }
            }
        }
    }
}