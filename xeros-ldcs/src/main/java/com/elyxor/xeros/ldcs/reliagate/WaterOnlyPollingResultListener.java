package com.elyxor.xeros.ldcs.reliagate;

/**
 * Created by will on 10/2/15.
 */
public interface WaterOnlyPollingResultListener {
    void onWaterFillComplete(int hotWater, int coldWater);
}
