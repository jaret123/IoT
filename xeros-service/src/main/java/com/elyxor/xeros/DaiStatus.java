package com.elyxor.xeros;

import java.sql.Timestamp;
import java.util.*;

import com.elyxor.xeros.model.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elyxor.xeros.model.ActiveDai;
import com.elyxor.xeros.model.Machine;
import com.elyxor.xeros.model.repository.ActiveDaiRepository;
import com.elyxor.xeros.model.repository.MachineRepository;
import com.elyxor.xeros.model.repository.StatusRepository;


@Transactional
@Service
public class DaiStatus {
	@Autowired ActiveDaiRepository activeDaiRepository;
    @Autowired StatusRepository statusRepository;
    @Autowired MachineRepository machineRepository;

	public boolean receivePing(String daiIdentifier) {
		List<ActiveDai> daiList = activeDaiRepository.findByDaiIdentifier(daiIdentifier);
		if (daiList != null && daiList.size() > 0) {
			ActiveDai dai = daiList.iterator().next();
			dai.setLastPing(new Timestamp(System.currentTimeMillis()));
			return true;
	 	}
		return false;
	}

	public String pingStatus() {
		long startTime = System.currentTimeMillis();
		Iterable<ActiveDai> daiList = activeDaiRepository.findAll();
		String output = "list of offline DAQs: ";
		for (ActiveDai dai : daiList) {
			if (dai.getLastPing() != null) {
				if ((System.currentTimeMillis() - dai.getLastPing().getTime()) > 3600000)
					output += dai.getDaiIdentifier()+", last ping at: " + dai.getLastPing()+"\n";
			}
		}
		if (!output.equals("list of offline DAQs: "))
			return output;
		else {
			output = "<pingdom_http_custom_check>\n\t<status>OK</status>\n\t<response_time>"
					+(System.currentTimeMillis()-startTime)+"</response_time>\n</pingdom_http_custom_check>";
			return output;
		}
	}
    public boolean receiveMachineStatus(String daiIdentifier, byte xerosStatus, byte stdStatus) {
        List<Machine> stdMachines = machineRepository.findByDaiDaiIdentifierAndMachineIdentifier(daiIdentifier, "Std");
        List<Machine> xerosMachines = machineRepository.findByDaiDaiIdentifierAndMachineIdentifier(daiIdentifier, "Xeros");
        String stdMessage = "";
        String xerosMessage = "";

        for (Machine machine : stdMachines) {
            createStatus(daiIdentifier, machine, stdStatus);
        }
        for (Machine machine : xerosMachines) {
            createStatus(daiIdentifier, machine, xerosStatus);
        }
        if (stdMachines!=null || xerosMachines!=null) {
            return true;
        }
        return false;
    }

    public List<Status> getStatus(List<Integer> machineIdList) {
        List<Status> statusList = new ArrayList<Status>();
        for (Integer id : machineIdList) {
            statusList.add(statusRepository.findByMachineId(id));
        }
        return statusList;
    }

    public List<Status> getStatusHistory(List<Integer> machineIdList){
        List<Status> statusList = new ArrayList<Status>();
        for (Integer id : machineIdList) {
            statusList.addAll(statusRepository.findHistoryByMachineId(id));
        }
        return statusList;
    }

    private Status createStatus(String daiIdentifier, Machine machine, int statusCode) {
        String message = "";
        Status status = new Status();
        status.setDaiIdentifier(daiIdentifier);
        status.setMachineId(machine.getId());
        status.setStatusCode(statusCode);
        status.setTimestamp(new Timestamp(System.currentTimeMillis()));

        if (statusCode == 0) message = "Machine is inactive.";
        else if (statusCode > 0) message = "Machine is active.";
        else if (statusCode == -1) message = "Unable to poll machine for status";
        else message = "Unknown status code.";

        status.setStatusMessage(message);
        statusRepository.save(status);
        return status;
    }
}
