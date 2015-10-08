package com.elyxor.xeros.ldcs.reliagate;

/**
 * Created by will on 9/16/15.
 */
public interface PollingResultListener {
    void onPortChanged(int portNum, int newValue);
}
