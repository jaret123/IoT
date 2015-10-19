package com.elyxor.xeros.ldcs.reliagate;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.*;
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

    private int mRef = 0;
    private int mCount = 16;

    private int[] mPreviousPortStatus = new int[mCount];

    public PollingRunnable(PollingResultListener listener, TCPMasterConnection connection, boolean isMock) {
        this.mListener = listener;
        this.mConnection = connection;
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

        mTransaction = new ModbusTCPTransaction(mConnection);
        mReadRequest = new ReadInputDiscretesRequest(mRef, mCount);

        mTransaction.setRequest(mReadRequest);

        while (true) {
            try {
                mTransaction.execute();
            } catch (ModbusException e) {
                e.printStackTrace();
            }
            ModbusResponse response = mTransaction.getResponse();
            if (response instanceof ExceptionResponse) {
                logger.warn(TAG, ((ExceptionResponse)response).getHexMessage());
            }
            else {
                mReadResponse = (ReadInputDiscretesResponse) mTransaction.getResponse();
                for (int i = 0; i < mCount; i++) {

                    //ignore "events" for water meters
                    if (i == 0 || i == 1 || i == 8 || i == 9) {
                        continue;
                    }

                    int value = mReadResponse.getDiscreteStatus(i) ? 1 : 0;
                    if (value != mPreviousPortStatus[i]) {
                        mListener.onPortChanged(i, value);
                    }
                    mPreviousPortStatus[i] = value;
                }
            }
        }
    }
}