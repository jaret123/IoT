package com.elyxor.xeros;

import com.elyxor.xeros.model.*;
import com.elyxor.xeros.model.repository.*;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
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
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;


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

    public List<Status> getStatusGaps(List<Machine> machineList){
        List<Status> statusList = new ArrayList<Status>();
        List<Status> result = new ArrayList<Status>();
        if (machineList.size() == 0) {
            machineList = (ArrayList<Machine>) machineRepository.findAll();
        }
        for (Machine machine : machineList) {
            statusList.addAll(statusRepository.findByMachineOrderByTimestampDesc(machine));
            for (int i = 0; i < statusList.size() - 1; i++) {
                Status current = statusList.get(i);
                Status prev = statusList.get(i + 1);
                long currentLong = ((ChronoLocalDateTime)current.getTimestamp().toLocalDateTime()).toEpochSecond(ZoneOffset.UTC);
                long prevLong = ((ChronoLocalDateTime)prev.getTimestamp().toLocalDateTime()).toEpochSecond(ZoneOffset.UTC);
                long diff = currentLong - prevLong;
                if (diff > 3600) {
                    result.add(statusList.get(i));
                    result.add(statusList.get(i+1));
                }
            }
            statusList.clear();
        }
        return result;
    }

    public File getStatusGaps() {
        List<Status> statusList = getStatusGaps(new ArrayList<Machine>());
        String output = "";
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(getCurrentDateAndTime());
        initializeStatusGapSheet(sheet);
        int k = 1;
        for (int i = 0; i < statusList.size() - 1; i++) {
            Status current = statusList.get(i);
            Status prev = statusList.get(i+1);
            HSSFRow row = sheet.createRow(k);

            String company = current.getMachine().getLocation().getCompany().getName();
            String location = current.getMachine().getLocation().getName();
            String machine = current.getMachine().getName();
            String disconnected = prev.getTimestamp().toString();
            String reconnected = current.getTimestamp().toString();

            long currentLong = ((ChronoLocalDateTime)current.getTimestamp().toLocalDateTime()).toEpochSecond(ZoneOffset.UTC);
            long prevLong = ((ChronoLocalDateTime)prev.getTimestamp().toLocalDateTime()).toEpochSecond(ZoneOffset.UTC);
            long diff = currentLong - prevLong;

            String diffString = String.format("%02d:%02d:%02d", TimeUnit.SECONDS.toHours(diff),
                    TimeUnit.SECONDS.toMinutes(diff) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.SECONDS.toSeconds(diff) % TimeUnit.MINUTES.toSeconds(1));

            row.createCell(0).setCellValue(company);
            row.createCell(1).setCellValue(location);
            row.createCell(2).setCellValue(machine);
            row.createCell(3).setCellValue(disconnected);
            row.createCell(4).setCellValue(reconnected);
            row.createCell(5).setCellValue(diffString);

            k++;
            i++;
        }
        if (sheet != null) {
            for (int j=0; j<sheet.getRow(0).getPhysicalNumberOfCells(); j++) {
                sheet.autoSizeColumn(j);
            }
        }
        return writeFile(workbook);

    }

    public File getLastLog() {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = null;
        int i = 1;

        for (Machine machine : machineRepository.findAll()) {
            Integer machineId = machine.getId();
            Cycle cycle = cycleRepository.findLastCycleByMachine(machineId);
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
                i++;
            }
        }
        if (sheet != null) {
            for (int j=0; j<sheet.getRow(0).getPhysicalNumberOfCells(); j++) {
                sheet.autoSizeColumn(j);
            }
        }

        return writeFile(workbook);
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
        else {
            return getCycleReports(from, to, Integer.parseInt(exception));
        }
    }

    public File getCycleReportsForMachine(String startDate, String endDate, String exceptionType, String machineId) {
        List<Machine> machineList = new ArrayList<Machine>();
        int exception;
        if (!machineId.isEmpty()) {
            machineList.add(machineRepository.findById(Integer.parseInt(machineId)));
        }
        if (exceptionType.isEmpty()) {
            exception = 0;
        }
        exception = Integer.parseInt(exceptionType);
        return getCycleReports(machineList, startDate, endDate, exception);
    }
    public File getCycleReports(String date) {
        return getCycleReports(date, date, 0);
    }
    public File getCycleReports(String startDate, String endDate, Integer exceptionType) {
        return getCycleReports(machineMappingRepository.findAll(), startDate, endDate, exceptionType);
    }

    //    @Scheduled(cron = "* * */2 * * *")
    public void checkStatusTime() {
        List<Integer> machineIds = machineRepository.findAllMachineIds();
        List<Status> statusList = getStatus(machineIds);

        for (Status status : statusList) {
            if (status != null && status.getTimestamp().before(getTimestampForIdleInterval())) {
                Machine machine =  status.getMachine();
                createStatus(status.getDaiIdentifier(), machine, -2);
            }
        }
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
            statusList.addAll(statusRepository.findHistoryByMachineIdWithLimit(id, 100));
        }
        return statusList;
    }

    private Status createStatus(String daiIdentifier, Machine machine, int statusCode) {
        String message = "";
        Status status = new Status();
        status.setDaiIdentifier(daiIdentifier);
        status.setMachine(machine);
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

    private void initializeLogSheet(HSSFSheet sheet) {
        HSSFRow header = sheet.createRow(0);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        HSSFFont font = sheet.getWorkbook().createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);

        style.setBorderBottom(CellStyle.BORDER_MEDIUM);
        style.setFont(font);

        header.createCell(0).setCellValue("Company Name");
        header.createCell(1).setCellValue("Location Name");
        header.createCell(2).setCellValue("Machine Name");
        header.createCell(3).setCellValue("Last Cycle");

        for (int i = 0; i < 4; i++) {
            header.getCell(i).setCellStyle(style);
        }
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
    private void initializeStatusGapSheet(HSSFSheet sheet) {
        HSSFRow header = sheet.createRow(0);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        HSSFFont font = sheet.getWorkbook().createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setBorderBottom(CellStyle.BORDER_MEDIUM);
        style.setFont(font);

        header.createCell(0).setCellValue("Company Name");
        header.createCell(1).setCellValue("Location Name");
        header.createCell(2).setCellValue("Machine Name");
        header.createCell(3).setCellValue("Disconnected");
        header.createCell(4).setCellValue("Reconnected");
        header.createCell(5).setCellValue("Gap Length");

        for (int i=0; i<6; i++) {
            header.getCell(i).setCellStyle(style);
        }
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
    private Timestamp getTimestampForLastLog() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -24);
        return new Timestamp(c.getTimeInMillis());
    }
    private Timestamp getTimestampForIdleInterval() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -4);
        return new Timestamp(c.getTimeInMillis());
    }
    private String getCurrentDateAndTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH.mm.ss z");

        return sdf.format(c.getTime());
    }
}

