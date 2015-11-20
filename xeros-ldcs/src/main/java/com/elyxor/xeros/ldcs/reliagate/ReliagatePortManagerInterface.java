package com.elyxor.xeros.ldcs.reliagate;

import com.elyxor.xeros.ldcs.thingworx.ThingWorxClient;

/**
 * Created by will on 9/21/15.
 */
public interface ReliagatePortManagerInterface {
    void init();
    public void setThingWorxClient(ThingWorxClient client);
    public ReliagatePort[] getReliagatePorts();
}
