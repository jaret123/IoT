package com.elyxor.xeros.ldcs.reliagate;

import com.elyxor.xeros.ldcs.thingworx.ThingWorxClient;

/**
 * Created by will on 9/21/15.
 */
public class MockReliagatePortManager implements ReliagatePortManagerInterface {

    private ThingWorxClient mClient;

    @Override public void init() {
        ReliagatePort port = new ReliagatePort(null, 1, mClient);
        port.startPolling(true);
    }

    @Override public void setThingWorxClient(ThingWorxClient client) {
        this.mClient = client;
    }
}
