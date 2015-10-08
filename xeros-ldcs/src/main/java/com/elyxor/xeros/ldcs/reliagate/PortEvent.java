package com.elyxor.xeros.ldcs.reliagate;

import org.joda.time.DateTime;

/**
 * Created by will on 9/16/15.
 */
public class PortEvent {
    private int mPortNum;
    private EventType mType;
    private DateTime mTimetamp;
    private long mDuration;

    public PortEvent(int portNum, EventType type, DateTime timestamp, long duration) {
        this.mPortNum = portNum;
        this.mType = type;
        this.mTimetamp = timestamp;
        this.mDuration = duration;
    }

    public int getPortNum() {
        return mPortNum;
    }

    public void setPortNum(int portNum) {
        mPortNum = portNum;
    }

    public EventType getType() {
        return mType;
    }

    public void setType(EventType type) {
        mType = type;
    }

    public DateTime getTimestamp() {
        return mTimetamp;
    }

    public void setTimetamp(DateTime timetamp) {
        mTimetamp = timetamp;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }
}
