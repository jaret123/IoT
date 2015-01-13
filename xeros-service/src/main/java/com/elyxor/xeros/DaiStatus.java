package com.elyxor.xeros;

import com.elyxor.xeros.model.*;
import com.elyxor.xeros.model.repository.*;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


@Transactional
@Service
public class DaiStatus {
	@Autowired ActiveDaiRepository activeDaiRepository;
    @Autowired StatusRepository statusRepository;
    @Autowired MachineRepository machineRepository;
    @Autowired ApplicationConfig appConfig;
    @Autowired MachineMappingRepository machineMappingRepository;
    @Autowired DaiMeterActualRepository meterActualRepository;
    @Autowired CycleRepository cycleRepository;
    @Autowired DaiMeterCollectionRepository collectionRepository;

    DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    DateTimeFormatter dateAndTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    DecimalFormat decimalFormat = new DecimalFormat("#.##");

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
            statusList.addAll(statusRepository.findHistoryByMachineId(id, 100));
        }
        return statusList;
    }

    public List<Status> getStatusGaps(List<Integer> machineIdList){
        List<Status> statusList = new ArrayList<Status>();
        List<Status> result = new ArrayList<Status>();
        if (machineIdList.size() == 0) {
            machineIdList = machineRepository.findAllMachineIds();
        }
        for (Integer id : machineIdList) {
            statusList.addAll(statusRepository.findHistoryByMachineId(id, 1000000000));
            for (int i = 0; i < statusList.size() - 1; i++) {
                Status current = statusList.get(i);
                Status prev = statusList.get(i+1);
                long diff = current.getTimestamp().getTime() - prev.getTimestamp().getTime();
                if (diff > 3600000) {
                    result.add(statusList.get(i));
                    result.add(statusList.get(i+1));
                }
            }
            statusList.clear();
        }
        return result;
    }

    public String getStatusGaps() {
        List<Status> statusList = getStatusGaps(new ArrayList<Integer>());
        String output = "";
        for (Status status : statusList) {
            output += "Machine ID: " + status.getMachineId() + ", DAI Identifier: " + status.getDaiIdentifier() + ", Timestamp:" + status.getTimestamp().toString() + "\n";
        }
        return output;

    }

//    @Scheduled(cron = "* * */2 * * *")
    public void checkStatusTime() {
        List<Integer> machineIds = machineRepository.findAllMachineIds();
        List<Status> statusList = getStatus(machineIds);

        for (Status status : statusList) {
            if (status != null && status.getTimestamp().before(getTimestampForIdleInterval())) {
                Machine machine =  machineRepository.findById(status.getMachineId());
                createStatus(status.getDaiIdentifier(), machine, -2);
            }
        }
    }

    public File getCycleReports(UriInfo info) {
        String from = info.getPathParameters().getFirst("fromDate");
        String to = info.getPathParameters().getFirst("toDate");
        String exception = info.getPathParameters().getFirst("exception");
        String machine = info.getPathParameters().getFirst("machine");
        String company = info.getPathParameters().getFirst("company");
        String location = info.getPathParameters().getFirst("location");

        if (to == null) {
            to = from;
        }
        if (exception == null) {
            exception = 0 + "";
        }
        if (machine != null) {
            return getCycleReportsForMachine(from, to, exception, machine);
        }
//        else if (company != null) {
////            return getCycleReportsForCompany(from, to, exception, company);
//        }
//        else if (location != null) {
////            return getCycleReportsForLocation(from, to, exception, location);
//        }
        else {
            return getCycleReports(from, to, Integer.parseInt(exception));
        }
    }

    public File getCycleReportsForMachine(String startDate, String endDate, String exceptionType, String machineId) {
//        return getCycleReports(, startDate, endDate, Integer.parseInt(exceptionType));
        return new File("file");
    }

    public File getCycleReports(String date) {

        return getCycleReports(date, date, 0);
//        Iterable<MachineMapping> machineList = machineMappingRepository.findAll();
//
//        PreparedStatement statement = null;
//        ResultSet rs = null;
//        HSSFWorkbook workbook = new HSSFWorkbook();
//        FileOutputStream out = null;
//        File file = null;
//        HSSFSheet sheet = workbook.createSheet();
//        for (MachineMapping machineMapping : machineList) {
//            try {
//                Connection connection = appConfig.dataSource().getConnection();
//                statement = connection.prepareStatement(QUERY_SIMPLE_CYCLE);
//                statement.setDate(1, Date.valueOf(date));
//                statement.setInt(2, machineMapping.getDaiId());
//                rs = statement.executeQuery();
//                while (rs != null && rs.next()) {
//                    if (rs.first()) {
//                        sheet = workbook.createSheet(rs.getString(INDEX_MACHINE) + " " + rs.getString(INDEX_DATE));
//                        HSSFRow rowHeader = sheet.createRow(0);
//                        rowHeader.createCell(0).setCellValue("Company Name");
//                        rowHeader.createCell(1).setCellValue("Location Name");
//                        rowHeader.createCell(2).setCellValue("Machine Name");
//                        rowHeader.createCell(3).setCellValue("Classification Name");
//                        rowHeader.createCell(4).setCellValue("Reading Date");
//                        rowHeader.createCell(5).setCellValue("Cycle Start Time");
//                        rowHeader.createCell(6).setCellValue("Cycle End Time");
//                        rowHeader.createCell(7).setCellValue("Water Sewer");
//                        rowHeader.createCell(8).setCellValue("Hot Water");
//                        rowHeader.createCell(9).setCellValue("Therms");
//                        rowHeader.createCell(10).setCellValue("Run Time");
//                    }
//                    HSSFRow row = sheet.createRow(rs.getRow());
//                    row.createCell(0).setCellValue(rs.getString(INDEX_COMPANY));
//                    row.createCell(1).setCellValue(rs.getString(INDEX_LOCATION));
//                    row.createCell(2).setCellValue(rs.getString(INDEX_MACHINE));
//                    row.createCell(3).setCellValue(rs.getString(INDEX_CLASSIFICATION));
//                    row.createCell(4).setCellValue(rs.getString(INDEX_DATE));
//                    row.createCell(5).setCellValue(rs.getString(INDEX_START));
//                    row.createCell(6).setCellValue(rs.getString(INDEX_END));
//                    row.createCell(10).setCellValue(Double.valueOf(rs.getString(INDEX_RUN_TIME)));
//
//                    List<Cycle> ekCycles = cycleRepository.findCyclesForCycleTime(machineMapping.getEkId(), rs.getTimestamp(INDEX_START), rs.getTimestamp(INDEX_END));
//                    if (ekCycles.size() > 0) {
//                        Float coldWater = 0f;
//                        Float hotWater = 0f;
//                        Float therms = 0f;
//                        for (Cycle cycle : ekCycles) {
//                            coldWater += cycle.getColdWaterVolume();
//                            hotWater += cycle.getHotWaterVolume();
//                            therms += cycle.getTherms();
//                        }
//                        row.createCell(7).setCellValue(coldWater);
//                        row.createCell(8).setCellValue(hotWater);
//                        row.createCell(9).setCellValue(therms);
//                    }
//                    else {
//                        row.createCell(7).setCellValue(Double.valueOf(rs.getString(INDEX_WATER_SEWER)));
//                        row.createCell(8).setCellValue(Double.valueOf(rs.getString(INDEX_WATER_HOT)));
//                        row.createCell(9).setCellValue(Double.valueOf(rs.getString(INDEX_THERMS)));
//                    }
//                }
//                rs.close();
//                for (int j=0; j < 10; j++) {
//                    sheet.autoSizeColumn(j);
//                }
//                statement.close();
//                connection.close();
//            } catch (SQLException sqle) {
//            //handle exception
//            } catch (Exception e) {
//                //handle exception
//            }
//
//        }
//        try {
//            file = new File("output.xls");
//            out = new FileOutputStream(file);
//            workbook.write(out);
//            return file;
//        } catch (FileNotFoundException fnfe) {
//            //handle
//        } catch (IOException ioe) {
//            //handle
//        }
//        return file;
    }
    public File getCycleReports(String startDate, String endDate, Integer exceptionType) {
        return getCycleReports(machineMappingRepository.findAll(), startDate, endDate, exceptionType);
    }
    public File getCycleReports(Iterable<MachineMapping> machineList, String startDate, String endDate, Integer exceptionType) {
        HSSFWorkbook workbook = new HSSFWorkbook();
        FileOutputStream out = null;
        File file = null;
        HSSFSheet sheet = null;

        for (MachineMapping machineMapping : machineList) {
            Machine machine = machineRepository.findById(machineMapping.getDaiId());
            List<Cycle> cycleList = processException(startDate, endDate, machine, exceptionType);
            if (cycleList != null) {
                Location location = machine.getLocation();
                String machineName = machine.getName();
                String locationName = location.getName();
                for (int i = 0; i < cycleList.size(); i++) {
                    Cycle cycle = cycleList.get(i);
                    if (sheet == null) {
                        sheet = workbook.createSheet(locationName + " " + machineName);
                        initializeSheet(sheet);
                    }

                    DateTime readingDateTime = new DateTime(cycle.getReadingTimestamp());
                    DateTime startTime = readingDateTime.minusMinutes(Math.round(cycle.getRunTime()));

                    String readingDate = readingDateTime.toString(dateFormatter);
                    String cycleEndTime = readingDateTime.toString(dateAndTimeFormatter);
                    String cycleStartTime = startTime.toString(dateAndTimeFormatter);


                    HSSFRow row = sheet.createRow(i+1);
                    row.createCell(1).setCellValue(cycle.getLocation().getName());
                    Company company = cycle.getLocation().getCompany();
                    row.createCell(0).setCellValue(company.getName());
                    row.createCell(2).setCellValue(machine.getName());
                    row.createCell(3).setCellValue(cycle.getClassification().getName());
                    row.createCell(4).setCellValue(readingDate);
                    row.createCell(5).setCellValue(cycleStartTime);
                    row.createCell(6).setCellValue(cycleEndTime);
                    row.createCell(10).setCellValue(Double.valueOf(decimalFormat.format(cycle.getRunTime())));

                    List<Cycle> ekCycles = cycleRepository.findCyclesForCycleTime(machineMapping.getEkId(), new Timestamp(startTime.getMillis()), cycle.getReadingTimestamp());
                    if (ekCycles.size() > 0) {
                        Float coldWater = 0f;
                        Float hotWater = 0f;
                        Float therms = 0f;
                        for (Cycle ekCycle : ekCycles) {
                            coldWater += ekCycle.getColdWaterVolume();
                            hotWater += ekCycle.getHotWaterVolume();
                            therms += ekCycle.getTherms();
                        }
                        row.createCell(7).setCellValue(Double.valueOf(decimalFormat.format(coldWater)));
                        row.createCell(8).setCellValue(Double.valueOf(decimalFormat.format(hotWater)));
                        row.createCell(9).setCellValue(Double.valueOf(decimalFormat.format(therms)));
                    }
                    else {
                        row.createCell(7).setCellValue(Double.valueOf(decimalFormat.format(cycle.getColdWaterVolume())));
                        row.createCell(8).setCellValue(Double.valueOf(decimalFormat.format(cycle.getHotWaterVolume())));
                        row.createCell(9).setCellValue(Double.valueOf(decimalFormat.format(cycle.getTherms())));
                    }

                }
            }
            if (sheet != null) {
                for (int j=0; j < sheet.getRow(0).getPhysicalNumberOfCells(); j++) {
                    sheet.autoSizeColumn(j);
                }
            }
            sheet = null;
        }
        try {
            file = new File("output.xls");
            out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
            return file;
        } catch (FileNotFoundException fnfe) {
            //handle
        } catch (IOException ioe) {
            //handle
        }
        return file;
    }

    private List<Cycle> processException(String startDate, String endDate, Machine machine, Integer exceptionType) {
        List<Cycle> result = new ArrayList<Cycle>();
        Timestamp start = Timestamp.valueOf(startDate + " " + "00:00:00");
        Timestamp end = Timestamp.valueOf(endDate + " " + "23:59:59");
        Integer id = machine.getId();
        switch (exceptionType) {
            case 0: {
                //no exceptions
                result = cycleRepository.findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionLike(id, start, end, "");
                break;
            }
            case 1: {
                result = cycleRepository.findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionLike(id, start, end, "%1%");
                break;
            }
            case 2: {
                result = cycleRepository.findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionLike(id, start, end, "%2%");
                break;
            }
            case 3: {
                result = cycleRepository.findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionLike(id, start, end, "%3%");
                break;
            }
            case 4: {
                result = cycleRepository.findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionLike(id, start, end, "%4%");
                break;
            }
            case 5: {
                result = cycleRepository.findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionLike(id, start, end, "%5%");
                break;
            }
            case 6: {
                result = cycleRepository.findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionLike(id, start, end, "%6%");
                break;
            }
            case -1: {
                //exceptions and non-exceptions
                result = cycleRepository.findByMachineIdAndReadingTimestampBetween(id, start, end);
                break;
            }
            case -2: {
                //all exceptions
                result = cycleRepository.findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionNotLike(id, start, end, "");
                break;
            }
        }
        return result;
    }

    private void initializeSheet(HSSFSheet sheet) {
        HSSFRow rowHeader = sheet.createRow(0);
        rowHeader.createCell(0).setCellValue("Company Name");
        rowHeader.createCell(1).setCellValue("Location Name");
        rowHeader.createCell(2).setCellValue("Machine Name");
        rowHeader.createCell(3).setCellValue("Classification Name");
        rowHeader.createCell(4).setCellValue("Reading Date");
        rowHeader.createCell(5).setCellValue("Cycle Start Time");
        rowHeader.createCell(6).setCellValue("Cycle End Time");
        rowHeader.createCell(7).setCellValue("Water Sewer");
        rowHeader.createCell(8).setCellValue("Hot Water");
        rowHeader.createCell(9).setCellValue("Therms");
        rowHeader.createCell(10).setCellValue("Run Time");
    }

    private Timestamp getTimestampForIdleInterval() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -4);
        return new Timestamp(c.getTimeInMillis());
    }

    private Timestamp getTimestampForLastLog() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -24);
        return new Timestamp(c.getTimeInMillis());
    }

    private String getCurrentDateAndTime() {
        Calendar c = Calendar.getInstance();
        return c.getTime().toString();
    }

    public File getLastLog() {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = null;
        int i = 1;

        for (Machine machine : machineRepository.findAll()) {
            Integer machineId = machine.getId();
            Cycle cycle = cycleRepository.findLastByMachineIdByOrderByReadingTimestamp(machineId);
            if (cycle != null && cycle.getReadingTimestamp().before(getTimestampForLastLog())) {
                if (sheet == null) {
                    sheet = workbook.createSheet(getCurrentDateAndTime());
                    initializeLogSheet(sheet);
                }
                HSSFRow row = sheet.createRow(i);
                String company = machine.getLocation().getCompany().getName();
                String location = machine.getLocation().getName();
                String machineName = machine.getName();
                String lastLog = cycle.getReadingTimestamp().toString();

                row.createCell(0).setCellValue(company);
                row.createCell(1).setCellValue(location);
                row.createCell(2).setCellValue(machineName);
                row.createCell(3).setCellValue(lastLog);
            }
        }
        if (sheet != null) {
            for (int j=0; j<sheet.getRow(0).getPhysicalNumberOfCells(); j++) {
                sheet.autoSizeColumn(j);
            }
        }

        return writeFile(workbook);
    }

    private File writeFile(HSSFWorkbook workbook) {
        File file = new File("output.xls");
        try {
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
        } catch (FileNotFoundException e) {
            //handle
        } catch (IOException ex) {
            //handle
        }
        return file;
    }

    private void initializeLogSheet(HSSFSheet sheet) {
        HSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue("Company Name");
        header.createCell(0).setCellValue("Location Name");
        header.createCell(0).setCellValue("Machine Name");
        header.createCell(0).setCellValue("Last Log Date And Time");
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
        else if (statusCode == -2) message = "Machine is disconnected.";
        else message = "Unknown status code.";

        status.setStatusMessage(message);
        statusRepository.save(status);
        return status;
    }
    private final int INDEX_COMPANY = 1;
    private final int INDEX_LOCATION = 2;
    private final int INDEX_MACHINE = 3;
    private final int INDEX_CLASSIFICATION = 4;
    private final int INDEX_DATE = 5;
    private final int INDEX_START = 6;
    private final int INDEX_END = 7;
    private final int INDEX_WATER_SEWER = 8;
    private final int INDEX_WATER_HOT = 9;
    private final int INDEX_THERMS = 10;
    private final int INDEX_RUN_TIME = 11;

    private final int[] MACHINE_ID_LIST = {8, 9, 10, 11};
    private final String QUERY_SIMPLE_CYCLE =
            "select company.name as 'Company Name'," +
            "loc.location_name as 'Location Name'," +
            "m.machine_name as 'Machine Name'," +
            "class.name as 'Classification Name'," +
            "date(c.reading_timestamp) as 'Reading Date'," +
            "c.reading_timestamp - INTERVAL c.cycle_time_run_time MINUTE as 'Cycle Start Time'," +
            "c.reading_timestamp as 'Cycle End Time'," +
            "c.cycle_cold_water_volume as 'Water Sewer'," +
            "c.cycle_hot_water_volume as 'Hot Water'," +
            "c.cycle_therms as 'Therms'," +
            "c.cycle_time_run_time as 'Run Time' " +
            "from " +
            "xeros_cycle as c " +
            "LEFT JOIN xeros_machine as m " +
            "on c.machine_id = m.machine_id " +
            "LEFT JOIN xeros_classification as class " +
            "on c.classification_id = class.classification_id " +
            "LEFT JOIN xeros_location as loc " +
            "on c.location_id = loc.location_id " +
            "LEFT JOIN xeros_company as company " +
            "on loc.company_id = company.company_id " +
            "WHERE " +
            "dai_meter_actual_id in (" +
            "select " +
            "dai_meter_actual_id " +
            "from " +
            "xeros_dai_meter_collection " +
            "WHERE " +
            "date(c.reading_timestamp) = ? " +
            "AND m.machine_id = ? " +
            "AND company.name not like 'White Rose' " +
            "AND company.name not like 'Xeros'" +
            ") " +
            "ORDER BY dai_meter_actual_id;";

}
