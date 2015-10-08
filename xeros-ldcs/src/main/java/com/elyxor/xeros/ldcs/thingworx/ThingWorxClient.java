package com.elyxor.xeros.ldcs.thingworx;
import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.common.SecurityClaims;

public class ThingWorxClient extends ConnectedThingClient {

	public ThingWorxClient(ClientConfigurator config) throws Exception {
		super(config);
	}
	
	public static ThingWorxClient initConnection() throws Exception {
		
		// Set the required configuration information
		ClientConfigurator config = new ClientConfigurator();

		// The uri for connecting to Thingworx
		config.setUri("wss://54.162.102.138:443/Thingworx/WS");
		
		// Reconnect every 15 seconds if a disconnect occurs or if initial connection cannot be made
		config.setReconnectInterval(15);
				
		// Set the security using an Application Key
		String appKey = "57dedf9d-2cea-4b43-b8d8-751126ba76cb";
		SecurityClaims claims = SecurityClaims.fromAppKey(appKey);
		config.setSecurityClaims(claims);
				
		// Set the name of the client
		config.setName("XerosGateway");
		// This client is a SDK
		config.setAsSDKType();

		// Create the client passing in the configuration from above
		ThingWorxClient client = new ThingWorxClient(config);
		
	    //create virtual things and bind thing
		//SteamSensorThing SensorThing = new SteamSensorThing("SteamSensor1","Steam Sensor #" + 1,"SN000" + 1,client);
		//client.bindThing(SensorThing);
		

		try {
			// Start the client
			client.start();
		}
		catch(Exception eStart) {
			System.out.println("Initial Start Failed : " + eStart.getMessage());
		}
		
        return client;
	}
}
