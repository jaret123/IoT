package com.elyxor.xeros.ldcs.reliagate;


import com.elyxor.xeros.ldcs.AppConfiguration;
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

    Integer portCount = AppConfiguration.getReliagatePortCount();
    Boolean isGlobal = AppConfiguration.isGlobalController();

    /* Variables for storing the parameters */
    InetAddress[] addresses = new InetAddress[portCount]; //the slave's address
    TCPMasterConnection[] connections = new TCPMasterConnection[portCount];
    ReliagatePort[] ports = new ReliagatePort[portCount];

    InetAddress[] globalAddresses = new InetAddress[portCount];
    TCPMasterConnection[] globalConnections = new TCPMasterConnection[portCount];
    GlobalControllerPort[] globalPorts = new GlobalControllerPort[portCount];

    int port = Modbus.DEFAULT_PORT;

    public ThingWorxClient mClient;

    @Override public void init() {
        logger.info("Reliagate Port Manager Init: PortCount: " + portCount);

        if (AppConfiguration.isGlobalController()) {
            try {
                for (int i = 0; i < portCount; i++) {
                    logger.info("Reliagate GC Port Init: Adding Port: " + i);

                    int addr = 150 + i;
                    globalAddresses[i] = InetAddress.getByName("192.168.127." + addr);
                    globalConnections[i] = new TCPMasterConnection(globalAddresses[i]);
                    globalConnections[i].setPort(port);
                    logger.info("Reliagate Port Manager Init: Connecting Port with address: " + globalAddresses[i]);
                    logger.info("Reliagate Port Manager Init: Connecting Port with connection: " + globalConnections[i]);
                    logger.info("Reliagate Port Manager Init: Connecting Port with port: " + globalConnections[i].getPort());

                    globalConnections[i].connect();

                    globalPorts[i] = new GlobalControllerPort(this, globalConnections[i], i + 1, mClient);
                    logger.info("Reliagate Port Manager Init: Connected, beginning Polling");

                    globalPorts[i].startPolling(false);
                }
            } catch (Exception e) {
                logger.warn(e.getMessage() != null ? e.getMessage() : "");
            }
        }

        try {
            for (int i = 0; i < portCount; i++) {
                logger.info("Reliagate Port Manager Init: Adding Port: " + i);

                int addr = 254 - i;
                addresses[i] = InetAddress.getByName("192.168.127."+addr);
                connections[i] = new TCPMasterConnection(addresses[i]);
                connections[i].setPort(port);
                logger.info("Reliagate Port Manager Init: Connecting Port with address: " + addresses[i]);
                logger.info("Reliagate Port Manager Init: Connecting Port with connection: " + connections[i]);
                logger.info("Reliagate Port Manager Init: Connecting Port with port: " + connections[i].getPort());

                connections[i].connect();

                ports[i] = new ReliagatePort(this, connections[i], i+1, mClient);
                logger.info("Reliagate Port Manager Init: Connected, beginning Polling");

                ports[i].startPolling(false);
            }
        } catch (Exception e) {
            logger.warn(e.getMessage() != null ? e.getMessage() : "");
        }
    }

    public void setThingWorxClient(ThingWorxClient client) {
        mClient = client;
    }

    public GlobalControllerPort[] getGlobalPorts() {
        return globalPorts;
    }

    public void setGlobalPorts(GlobalControllerPort[] globalPorts) {
        this.globalPorts = globalPorts;
    }

    public ReliagatePort[] getReliagatePorts() {
        return ports;
    }

    public void setReliagatePorts(ReliagatePort[] ports) {
        this.ports = ports;
    }
}
