package com.elyxor.xeros;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
        for (Machine machine : stdMachines) {
            Status status = new Status();
            status.setDaiIdentifier(daiIdentifier);
            status.setMachineId(machine.getId());
            status.setStatusCode((int) stdStatus);
            status.setTimestamp(new Timestamp(System.currentTimeMillis()));
            statusRepository.save(status);
        }
        for (Machine machine : xerosMachines) {
            Status status = new Status();
            status.setDaiIdentifier(daiIdentifier);
            status.setMachineId(machine.getId());
            status.setStatusCode((int) xerosStatus);
            status.setTimestamp(new Timestamp(System.currentTimeMillis()));
            statusRepository.save(status);
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

    public List<List<Status>> getStatusHistory(List<Integer> machineIdList){
        List<List<Status>> statusList = new ArrayList<List<Status>>();
        for (Integer id : machineIdList) {
            statusList.add(statusRepository.findHistoryByMachineId(id));
        }
        return statusList;
    }
}
