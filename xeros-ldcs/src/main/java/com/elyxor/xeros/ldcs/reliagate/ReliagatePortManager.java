package com.elyxor.xeros.ldcs.reliagate;


import com.elyxor.xeros.ldcs.thingworx.ThingWorxClient;
import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.net.TCPMasterConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * Created by will on 7/7/15.
 */
public class ReliagatePortManager implements ReliagatePortManagerInterface {
    /* The important instances of the classes mentioned before */
    TCPMasterConnection con = null; //the connection
    ModbusTCPTransaction trans = null; //the transaction
    ModbusRequest req = null; //the request
    ModbusResponse res = null; //the response

    private static final Logger logger = LoggerFactory.getLogger(ReliagatePortManager.class);

    /* Variables for storing thex parameters */
    InetAddress addr = null; //the slave's address
    int port = Modbus.DEFAULT_PORT;

    public ThingWorxClient mClient;

    @Override public void init() {
        try {
            addr = InetAddress.getByName("192.168.127.254");
            logger.info("address: " + addr);
            con = new TCPMasterConnection(addr);
            con.setPort(port);
            con.connect();
            logger.info("connected to port");

            ReliagatePort port = new ReliagatePort(con, 1, mClient);
            port.startPolling(false);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void setThingWorxClient(ThingWorxClient client) {
        mClient = client;
    }

}
