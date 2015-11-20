package com.elyxor.xeros.ldcs.reliagate;

import com.elyxor.xeros.ldcs.thingworx.ThingWorxClient;

/**
 * Created by will on 9/21/15.
 */
public class MockReliagatePortManager implements ReliagatePortManagerInterface {

    private ThingWorxClient mClient;

    @Override public void init() {
//        ReliagatePort port = new ReliagatePort(null, 1, mClient);
//        port.startPolling(true);

        GlobalControllerPort gcPort = new GlobalControllerPort(this, null, 1, mClient);
        gcPort.startPolling(true);
    }

    @Override public void setThingWorxClient(ThingWorxClient client) {
        this.mClient = client;
    }

    @Override public ReliagatePort[] getReliagatePorts() {
        return new ReliagatePort[0];
    }
}
