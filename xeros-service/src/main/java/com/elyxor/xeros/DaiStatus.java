package com.elyxor.xeros;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elyxor.xeros.model.ActiveDai;
import com.elyxor.xeros.model.repository.ActiveDaiRepository;

@Transactional
@Service
public class DaiStatus {
	@Autowired ActiveDaiRepository activeDaiRepository;

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
					output += dai.getDaiIdentifier()+"\n";
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
}
