package com.elyxor.xeros;

import com.elyxor.xeros.model.*;
import com.elyxor.xeros.model.repository.*;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;


@Transactional(readOnly = true)
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
    @Autowired LocationRepository locationRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired LocalStaticValueRepository lsvRepository;
    @Autowired XerosLocalStaticValueRepository xlsvRepository;
    @Autowired StaticValueRepository staticValueRepository;
    @Autowired LocationProfileRepository locationProfileRepository;

    @PersistenceContext private EntityManager em;

    private static Logger logger = LoggerFactory.getLogger(DaiStatus.class);

    private static final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final DateTimeFormatter dateAndTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("HH:mm:ss");
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private static final DecimalFormat percentageFormat = new DecimalFormat("#.#%");
    private DateTime lastCheck = null; //used to determine row separator for compare report
    private static Map<String, FormulaVariance> formulaVarianceList = new LinkedHashMap<String, FormulaVariance>();

    public class FormulaVariance {
        float varianceTotal;
        float manufacturerVarianceTotal;
        int dayCount;

        public FormulaVariance(float varianceTotal, float manufacturerVarianceTotal, int dayCount) {
            this.varianceTotal = varianceTotal;
            this.manufacturerVarianceTotal = manufacturerVarianceTotal;
            this.dayCount = dayCount;
        }

        public float getVarianceTotal() {
            return varianceTotal;
        }
        public void setVarianceTotal(float varianceTotal) {
            this.varianceTotal = varianceTotal;
        }
        public float getManufacturerVarianceTotal() {
            return manufacturerVarianceTotal;
        }
        public void setManufacturerVarianceTotal(float manufacturerVarianceTotal) {
            this.manufacturerVarianceTotal = manufacturerVarianceTotal;
        }
        public int getDayCount() {
            return dayCount;
        }
        public void setDayCount(int dayCount) {
            this.dayCount = dayCount;
        }
    }
    public FormulaVariance findFormulaVariance(String formula) {
        FormulaVariance result = null;
        if (!formulaVarianceList.isEmpty()) {
            result = formulaVarianceList.get(formula);
        }
        return result;
    }

    public boolean receiveMachineStatus(String daiIdentifier, byte xerosStatus, byte stdStatus) {
        List<Machine> stdMachines = machineRepository.findByDaiDaiIdentifierAndMachineIdentifier(daiIdentifier, "Std");
        List<Machine> xerosMachines = machineRepository.findByDaiDaiIdentifierAndMachineIdentifier(daiIdentifier, "Xeros");

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

    public File getStatusGaps(UriInfo info) {
        String startDate = info.getQueryParameters().getFirst("fromDate");
        String endDate = info.getQueryParameters().getFirst("toDate");

        if (startDate == null && endDate == null) {
            startDate = "2000-01-01";
            endDate = "2999-12-31";
        }
        if (endDate == null) {
            endDate = startDate;
        }

        List<Cycle> result = new ArrayList<Cycle>();
        Timestamp start = Timestamp.valueOf(startDate + " " + "00:00:00");
        Timestamp end = Timestamp.valueOf(endDate + " " + "23:59:59");

        List<Status> statusList = calculateStatusGaps(new ArrayList<Machine>(), start, end);
        String output = "";
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(getCurrentDateAndTime());
        initializeStatusGapSheet(sheet, getHeaderStyle(workbook));
        int k = 1;

        for (int i = 0; i < statusList.size() - 1; i++) {
            Status current = statusList.get(i);
            Status prev = statusList.get(i+1);
            HSSFRow row = sheet.createRow(k);
            Machine m;
            Location l;
            Company c;
            String location = "";
            String company = "";
            String machine = "";
            if ((m = machineRepository.findById(current.getMachineId())) != null) {
                machine = m.getName();
                if ((l = m.getLocation()) != null) {
                    location = l.getName();
                    if ((c = l.getCompany()) != null) {
                        company = c.getName();
                    }
                }
            }

            String disconnected = prev.getTimestamp().toString();
            String reconnected = current.getTimestamp().toString();

            long currentLong = calculateTimeLong(current);
            long prevLong = calculateTimeLong(prev);

            long diff = currentLong - prevLong;

            String diffString = formatTimeDiffString(diff);

            row.createCell(0).setCellValue(company);
            row.createCell(1).setCellValue(location);
            row.createCell(2).setCellValue(machine);
            row.createCell(3).setCellValue(disconnected);
            row.createCell(4).setCellValue(reconnected);
            row.createCell(5).setCellValue(diffString);

            statusList.remove(i);
            statusList.remove(i+1);
            k++;
            i++;
        }
        if (sheet != null) {
            for (int j=0; j<sheet.getRow(0).getPhysicalNumberOfCells(); j++) {
                sheet.autoSizeColumn(j);
            }
        }
        return writeFile(workbook, "statusGaps.xls");

    }
    public List<Status> calculateStatusGaps(List<Machine> machineList, Timestamp start, Timestamp end){
        List<Status> result = new ArrayList<Status>();

        long interval = 0;
        try {
            StaticValue staticValue = staticValueRepository.findByName("offline_interval");
            if (staticValue != null) {
                String intervalString = staticValue.getValue();
                interval = 60 * Long.parseLong(intervalString);
            }
        } catch (NullPointerException e) {
            String msg = "unable to convert interval to long, no offline_interval in db?";
            logger.warn(msg, e);
            return result;
        }
        Session session = null;
        Query query = null;
        ScrollableResults results = null;
        if (em != null) {
             session = em.unwrap(Session.class);
        }

        if (session != null) {
            query = session.createQuery("FROM Status WHERE time_stamp >= :start AND time_stamp <= :end ORDER BY machine_id asc, time_stamp desc");
        }
        if (query != null) {
            query.setParameter("start", start);
            query.setParameter("end", end);
            query.setReadOnly(true);
            query.setFetchSize(2);
            results = query.scroll(ScrollMode.FORWARD_ONLY);
        }
        int i = 0;
        while (results != null && results.next()) {
            Status current = (Status) results.get(0);
            if (!results.next()) {
                results.close();
                session.flush();
                break;
            }
            Status prev = (Status) results.get(0);
            long currentLong = calculateTimeLong(current);
            long prevLong = calculateTimeLong(prev);
            long diff = currentLong - prevLong;
            if (diff > interval) {
                result.add(current);
                result.add(prev);
            }
            session.evict(current);
            session.evict(prev);
        }
        return result;
    }

    public File getLastLog() {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = null;
        int i = 1;

        CellStyle redStyle = getRedHighlightStyle(workbook);
        CellStyle yellowStyle = getVarianceHighlightStyle(workbook);
        CellStyle headerStyle = getHeaderStyle(workbook);
        Timestamp lastLogRed = getTimestampForLastLogRedInterval();
        Timestamp lastLogYellow = getTimestampForLastLogYellowInterval();
        Timestamp heartbeatRed = getTimestampForHeartbeatRedInterval();
        Timestamp heartbeatYellow = getTimestampForHeartbeatYellowInterval();

        for (Machine machine : machineRepository.findAll()) {
            Integer machineId = machine.getId();
            Cycle cycle = cycleRepository.findLastCycleByMachine(machineId);
            Status status = statusRepository.findByMachineId(machineId);
            if (cycle != null) {
                if (sheet == null) {
                    sheet = workbook.createSheet(getCurrentDateAndTime());
                    initializeLogSheet(sheet, headerStyle);
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
                if (cycle.getReadingTimestamp().before(lastLogRed)) {
                    row.getCell(3).setCellStyle(redStyle);
                }
                else if (cycle.getReadingTimestamp().before(lastLogYellow)) {
                    row.getCell(3).setCellStyle(yellowStyle);
                }
                if (status != null) {
                    String statusTime = status.getTimestamp().toString();
                    row.createCell(4).setCellValue(statusTime);
                    if (status.getTimestamp().before(heartbeatRed)) {
                        row.getCell(4).setCellStyle(redStyle);
                    }
                    else if (status.getTimestamp().before(heartbeatYellow)) {
                        row.getCell(4).setCellStyle(yellowStyle);
                    }
                }
                i++;
            }
        }
        if (sheet != null) {
            for (int j=0; j<sheet.getRow(0).getLastCellNum(); j++) {
                sheet.autoSizeColumn(j);
            }
        }
        return writeFile(workbook, "lastLog.xls");
    }

    public File getCycleReports(UriInfo info) {
        String from = info.getQueryParameters().getFirst("fromDate");
        String to = info.getQueryParameters().getFirst("toDate");
        String exception = info.getQueryParameters().getFirst("exception");
        String machine = info.getQueryParameters().getFirst("machine");
        String company = info.getQueryParameters().getFirst("company");
        String location = info.getQueryParameters().getFirst("location");
        String type = info.getQueryParameters().getFirst("type");
        String unknown = info.getQueryParameters().getFirst("unknown");
        String manufacturer = info.getQueryParameters().getFirst("manufacturer");

        Integer exceptionId = 0;
        Integer unknownId = 0;
        boolean useManufacturer = manufacturer != null && manufacturer.equals("1");

        if (to == null) {
            to = from;
        }
        if (exception != null) {
            exceptionId = Integer.parseInt(exception);
        }
        if (unknown != null) {
            unknownId = Integer.parseInt(unknown);
        }
        if (type != null && type.equals("compare")){
            if (company != null) {
                Integer companyId = Integer.parseInt(company);
                Iterable<Location> locations = (companyRepository.findOne(companyId)).getLocations();
                List<Machine> machineList = new ArrayList<Machine>();
                for (Location locationItem : locations) {
                    machineList.addAll(locationItem.getMachines());
                }
                return createCompareReports(machineList, from, to, exceptionId, unknownId, useManufacturer);
            }
            else if (location != null) {
                Integer locationId = Integer.parseInt(location);
                Location locationItem = locationRepository.findOne(locationId);
                Iterable<Machine> machineList = locationItem.getMachines();
                return createCompareReports(machineList, from, to, exceptionId, unknownId, useManufacturer);
            }
            else if (machine != null) {
                Integer machineId = Integer.parseInt(machine);
                List<Machine> machineList = new ArrayList<Machine>();
                machineList.add(machineRepository.findOne(machineId));
                return createCompareReports(machineList, from, to, exceptionId, unknownId, useManufacturer);
            }
            else {
                return createCompareReports(machineRepository.findAll(), from, to, exceptionId, unknownId, useManufacturer);
            }
        }
        if (type != null && type.equals("ek")) {
            if (company != null) {
                Integer companyId = Integer.parseInt(company);
                Iterable<Location> locations = (companyRepository.findOne(companyId)).getLocations();
                List<Machine> machines = new ArrayList<Machine>();
                for (Location locationItem : locations) {
                    Iterable<Machine> machineList = locationItem.getMachines();
                    for (Machine machineItem : machineList) {
                        machines.add(machineItem);
                    }
                }
                return createEkCycleReports(machines, from, to, exceptionId, unknownId);
            }
            else if (location != null) {
                Integer locationId = Integer.parseInt(location);
                Location locationItem = locationRepository.findOne(locationId);
                Iterable<Machine> machineList = locationItem.getMachines();
                return createEkCycleReports(machineList, from, to, exceptionId, unknownId);
            }
            else if (machine != null) {
                Integer machineId = Integer.parseInt(machine);
                List<Machine> machineList = new ArrayList<Machine>();
                machineList.add(machineRepository.findById(machineId));
                return createEkCycleReports(machineList, from, to, exceptionId, unknownId);
            }
            else {
                return createEkCycleReports(machineRepository.findAll(), from, to, exceptionId, unknownId);
            }
        }
        else {
            if (company != null) {
                Integer companyId = Integer.parseInt(company);
                Company co = companyRepository.findOne(companyId);
                Iterable<Location> locations = co.getLocations();
                List<Machine> machineList = new ArrayList<Machine>();
                for (Location loc : locations) {
                    machineList.addAll(loc.getMachines());
                }
                return createCycleReports(machineList, from, to, exceptionId, unknownId);
            }
            else if (location != null) {
                Integer locationId = Integer.parseInt(location);
                Location loc = locationRepository.findOne(locationId);
                Iterable<Machine> machineList = loc.getMachines();
                return createCycleReports(machineList, from, to, exceptionId, unknownId);
            }
            else if (machine != null) {
                Integer machineId = Integer.parseInt(machine);
                List<Machine> machineList = new ArrayList<Machine>();
                machineList.add(machineRepository.findOne(machineId));
                return createCycleReports(machineList, from, to, exceptionId, unknownId);
            }
            else {
                return createCycleReports(machineRepository.findAll(), from, to, exceptionId, unknownId);
            }
        }
    }

    public File createEkCycleReports(Iterable<Machine> machineList, String startDate, String endDate, Integer exceptionType, Integer unknownId) {
        HSSFWorkbook workbook = new HSSFWorkbook();
        Sheet sheet = null;

        CellStyle headerStyle = getHeaderStyle(workbook);

        for (Machine machine : machineList) {
            List<Cycle> cycleList = processException(startDate, endDate, machine, exceptionType, unknownId);
            Location location;
            Company company;

            String locationName = "";
            String companyName = "";
            String machineName = machine.getName();
            MachineMapping machineMapping;
            Integer ekId = 0;

            if ((machineMapping = machineMappingRepository.findByDaiId(machine.getId())) != null) {
                ekId = machineMapping.getEkId();
            }

            if ((location = machine.getLocation()) != null) {
                locationName = location.getName();
                if ((company = location.getCompany()) != null) {
                    companyName = company.getName();
                }
            }

            if (cycleList != null) {
                for (int i = 0; i < cycleList.size(); i++) {
                    Cycle cycle = cycleList.get(i);
                    if (sheet == null) {
                        try {
                            sheet = workbook.createSheet(companyName.substring(0, 3) + " - " + locationName.substring(0, 3) + " - " + machineName);
                        } catch (Exception e) {
                            logger.warn("Could not create sheet: ", e.getMessage());
                            break;
                        }
                        initializeEkSheet(sheet, headerStyle);
                    }

                    DateTime readingDateTime = new DateTime(cycle.getReadingTimestamp());
                    DateTime startTime = readingDateTime.minusMinutes(Math.round(cycle.getRunTime()));

                    String readingDate = readingDateTime.toString(dateFormatter);
                    String cycleEndTime = readingDateTime.toString(dateAndTimeFormatter);
                    String cycleStartTime = startTime.toString(dateAndTimeFormatter);

                    String exception = "";
                    String className = "";
                    Integer intendedFormula = 0;
                    Classification classification;
                    DaiMeterActual actual;

                    if ((classification = cycle.getClassification())!=null){
                        className = classification.getName();
                    }
                    if ((actual = cycle.getDaiMeterActual())!=null) {
                        exception = actual.getException();
                    }
                    if (actual != null && classification != null && classification.getId() == 1) {
                        Integer formula = actual.getExpectedClassification();
                        intendedFormula = formula!=null?formula:0;
                    }


                    Row row = sheet.createRow(i+1);

                    row.createCell(0).setCellValue(companyName);
                    row.createCell(1).setCellValue(locationName);
                    row.createCell(2).setCellValue(machineName);
                    row.createCell(3).setCellValue(className);
                    row.createCell(4).setCellValue(readingDate);
                    row.createCell(5).setCellValue(cycleStartTime);
                    row.createCell(6).setCellValue(cycleEndTime);
                    row.createCell(11).setCellValue(formatDecimal(cycle.getRunTime()));
                    row.createCell(12).setCellValue(exception);
                    row.createCell(13).setCellValue(describeExceptions(exception));
                    if (intendedFormula != 0) {
                        row.createCell(15).setCellValue(intendedFormula);
                    }


                    List<Cycle> ekCycles = cycleRepository.findCyclesForCycleTime(ekId, new Timestamp(startTime.getMillis()), cycle.getReadingTimestamp());
                    if (ekCycles.size() > 0) {
                        Float coldWater = 0f;
                        Float hotWater = 0f;
                        Float therms = 0f;
                        for (Cycle ekCycle : ekCycles) {
                            Float cold = ekCycle.getColdWaterVolume()!=null?ekCycle.getColdWaterVolume():0;
                            Float hot = ekCycle.getHotWaterVolume()!=null?ekCycle.getHotWaterVolume():0;
                            Float therm = ekCycle.getTherms()!=null?ekCycle.getTherms():0;

                            coldWater += cold;
                            hotWater += hot;
                            therms += therm;
                        }
                        row.createCell(7).setCellValue(formatDecimal(coldWater));
                        row.createCell(8).setCellValue(formatDecimal(coldWater - hotWater));
                        row.createCell(9).setCellValue(formatDecimal(hotWater));
                        row.createCell(10).setCellValue(formatDecimal(therms));
                        row.createCell(14).setCellValue("X");
                    }
                    else {
                        Float total = cycle.getColdWaterVolume()!=null?cycle.getColdWaterVolume():0;
                        Float hot = cycle.getHotWaterVolume()!=null?cycle.getHotWaterVolume():0;
                        Float cold = total - hot;
                        Float therms = cycle.getTherms()!=null?cycle.getTherms():0;

                        row.createCell(7).setCellValue(formatDecimal(total));
                        row.createCell(8).setCellValue(formatDecimal(cold));
                        row.createCell(9).setCellValue(formatDecimal(hot));
                        row.createCell(10).setCellValue(formatDecimal(therms));
                    }

                }
            }
            sheet = null;
        }
        return writeFile(workbook, "ekCycleReport.xls");
    }
    public File createCycleReports(Iterable<Machine> machineList, String startDate, String endDate, Integer exceptionType, Integer unknownId) {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = null;

        CellStyle headerStyle = getHeaderStyle(workbook);

        for (Machine machine : machineList) {

            Location location;
            Company company;

            String locationName = "";
            String companyName = "";
            String machineName = machine.getName();

            if ((location = machine.getLocation()) != null) {
                locationName = location.getName();
                if ((company = location.getCompany()) != null) {
                    companyName = company.getName();
                }
            }

            List<Cycle> cycleList = processException(startDate, endDate, machine, exceptionType, unknownId);

            if (cycleList != null) {

                for (int i = 0; i < cycleList.size(); i++) {
                    Cycle cycle = cycleList.get(i);
                    if (sheet == null) {
                        String sheetName = companyName.substring(0,3) + " - " + locationName.substring(0,3) + " - " + machineName;
                        sheet = workbook.createSheet(sheetName);
                        initializeSheet(sheet, headerStyle);
                    }

                    DateTime readingDateTime = new DateTime(cycle.getReadingTimestamp());
                    DateTime startTime = readingDateTime.minusMinutes(Math.round(cycle.getRunTime()));

                    String readingDate = readingDateTime.toString(dateFormatter);
                    String cycleEndTime = readingDateTime.toString(dateAndTimeFormatter);
                    String cycleStartTime = startTime.toString(dateAndTimeFormatter);

                    String exception = "";
                    String className = "";
                    Integer intendedFormula = 0;
                    Classification classification;
                    DaiMeterActual actual;

                    if ((classification = cycle.getClassification())!=null){
                        className = classification.getName();
                    }
                    if ((actual = cycle.getDaiMeterActual())!=null) {
                        exception = actual.getException();
                    }
                    if (actual != null && classification != null && classification.getId() == 1) {
                        Integer formula = actual.getExpectedClassification();
                        intendedFormula = formula!=null?formula:0;
                    }

                    HSSFRow row = sheet.createRow(i+1);
                    row.createCell(1).setCellValue(locationName);
                    row.createCell(0).setCellValue(companyName);
                    row.createCell(2).setCellValue(machineName);
                    row.createCell(3).setCellValue(className);
                    row.createCell(4).setCellValue(readingDate);
                    row.createCell(5).setCellValue(cycleStartTime);
                    row.createCell(6).setCellValue(cycleEndTime);
                    row.createCell(11).setCellValue(formatDecimal(cycle.getRunTime()));
                    row.createCell(12).setCellValue(exception);
                    row.createCell(13).setCellValue(describeExceptions(exception));
                    if (intendedFormula != 0) {
                        row.createCell(14).setCellValue(intendedFormula);
                    }

                    Float total = cycle.getColdWaterVolume()!=null?cycle.getColdWaterVolume():0f;
                    Float hot = cycle.getHotWaterVolume()!=null?cycle.getHotWaterVolume():0f;
                    Float cold = total - hot;

                    row.createCell(7).setCellValue(formatDecimal(total));
                    row.createCell(8).setCellValue(formatDecimal(cold));
                    row.createCell(9).setCellValue(formatDecimal(hot));
                    row.createCell(10).setCellValue(formatDecimal(cycle.getTherms()));
                }
            }
            if (sheet != null) {
                for (int j=0; j < sheet.getRow(0).getLastCellNum(); j++) {
                    sheet.autoSizeColumn(j);
                }
            }
            sheet = null;
        }
        if (workbook.getNumberOfSheets() == 0) {
            return null;
        }
        return writeFile(workbook, "cycleReports.xls");
    }
    public File createCompareReports(Iterable<Machine> machines, String start, String end, Integer exceptionId, Integer unknownId, boolean manufacturer) {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = null;
        MachineMapping machineMapping = null;
        Integer ekId = 0;

        CellStyle headerStyle = getHeaderStyle(workbook);
        CellStyle underlineStyle = getUnderlineStyle(workbook);
        CellStyle moneyStyle = getMoneyStyle(workbook);
        CellStyle highlightStyle = getVarianceHighlightStyle(workbook);

        HSSFSheet summarySheet = workbook.createSheet("Summary");
        if (manufacturer) {
            initializeSummarySheetManufacturer(summarySheet, headerStyle);
        }
        else {
            initializeSummarySheet(summarySheet, headerStyle);
        }

        for (Machine machine : machines) {
            if ((machineMapping = machineMappingRepository.findOne(machine.getId())) != null) {
                ekId = machineMapping.getEkId();
            }

            Location location;
            Company company;

            String locationName = "";
            String companyName = "";
            String machineName = machine.getName();

            if ((location = machine.getLocation()) != null) {
                locationName = location.getName();
                if ((company = location.getCompany()) != null) {
                    companyName = company.getName();
                }
            }

            List<Cycle> cycleList = processException(start, end, machine, exceptionId, unknownId);
            if (cycleList != null) {

                LocationProfile profile = locationProfileRepository.findOne(location.getId());
                float costPerGallon = profile!=null?profile.getCostPerGallon():0f;

                HSSFRow machineNameRow = summarySheet.createRow(summarySheet.getLastRowNum()+1);
                machineNameRow.createCell(0).setCellValue(locationName);
                machineNameRow.createCell(1).setCellValue(machineName);
                for (Cell cell : machineNameRow)
                    cell.setCellStyle(headerStyle);
                float varianceTotal = 0f;
                float manufacturerVarianceTotal = 0f;

                for (Cycle cycle: cycleList) {
                    if (sheet == null) {
                        sheet = workbook.createSheet(companyName.substring(0,3) + " - " + locationName.substring(0,3) + " - " + machineName);

                        if (manufacturer) {
                            initializeCompareSheetManufacturer(sheet, headerStyle);
                        }
                        else {
                            initializeCompareSheet(sheet, headerStyle);
                        }
                        for (int j=0; j < sheet.getRow(0).getLastCellNum(); j++) {
                                sheet.autoSizeColumn(j);
                        }
                    }

                    DateTime readingDateTime = new DateTime(cycle.getReadingTimestamp());
                    DateTime startTime = readingDateTime.minusMinutes(Math.round(cycle.getRunTime()));

                    String readingDate = readingDateTime.toString(dateFormatter);
                    String cycleStartTime = startTime.toString(timeFormatter);

                    Classification classification;
                    DaiMeterActual actual;
                    Integer intendedFormula = 0;
                    String classificationName = "";

                    if ((classification = cycle.getClassification())!=null){
                        classificationName = classification.getName();
                    }
                    actual = cycle.getDaiMeterActual();

                    if (actual != null && classification != null && classification.getId() == 1) {
                        Integer formula = actual.getExpectedClassification();
                        intendedFormula = formula!=null?formula:0;
                    }

                    Float total = 0f;
                    Float comparison = getComparisonValue(cycle);
                    Float manufacturerComparison = 0f;

                    if (manufacturer) {
                        manufacturerComparison = getManufacturerComparisonValue(cycle);
                    }

                    HSSFRow row = sheet.createRow(sheet.getLastRowNum()+1);

                    if (checkDailySeperator(row, readingDateTime, underlineStyle)) {
                        for (int k=0; k < 7; k++)
                            row.createCell(k);

                        double variance = formatDecimal(varianceTotal);
                        double yearlyVariance = formatDecimal((float)(varianceTotal*365));
                        double monthlyVariance = formatDecimal((float)(varianceTotal*365/12));
                        double monthlySavings = formatDecimal((float)(varianceTotal*365/12*costPerGallon));

                        if (manufacturer) {
                            double manVariance = formatDecimal(manufacturerVarianceTotal);
                            double manYearly = formatDecimal((float)(manufacturerVarianceTotal*365));
                            double manMonthly = formatDecimal((float)(manufacturerVarianceTotal*365/12));
                            double manSavings = formatDecimal((float)(manufacturerVarianceTotal*365/12*costPerGallon));

                            row.createCell(11).setCellValue(variance);
                            row.createCell(12).setCellValue(yearlyVariance);
                            row.createCell(13).setCellValue(monthlyVariance);
                            row.createCell(14).setCellValue(monthlySavings);
                            row.getCell(14).setCellStyle(moneyStyle);

                            row.createCell(15).setCellValue(manVariance);
                            row.createCell(16).setCellValue(manYearly);
                            row.createCell(17).setCellValue(manMonthly);
                            row.createCell(18).setCellValue(manSavings);
                            row.getCell(18).setCellStyle(moneyStyle);
                            manufacturerVarianceTotal = 0f;
                        }
                        else {

                            row.createCell(8).setCellValue(variance);
                            row.createCell(9).setCellValue(yearlyVariance);
                            row.createCell(10).setCellValue(monthlyVariance);
                            row.createCell(11).setCellValue(monthlySavings);
                            row.getCell(11).setCellStyle(moneyStyle);
                        }
                        varianceTotal = 0f;
                        row = sheet.createRow(sheet.getLastRowNum()+1);
                    }

                    row.createCell(0).setCellValue(readingDate);
                    row.createCell(1).setCellValue(classificationName);
                    row.createCell(2).setCellValue(cycleStartTime);
                    row.createCell(4).setCellValue(comparison);

                    if (manufacturer) {
                        if (intendedFormula != 0) {
                            row.createCell(10).setCellValue(intendedFormula);
                        }
                        row.createCell(8).setCellValue(manufacturerComparison);
                    }
                    else {
                        if (intendedFormula != 0) {
                            row.createCell(7).setCellValue(intendedFormula);
                        }
                    }

                    if (machineMapping != null) {
                        List<Cycle> ekCycles = cycleRepository.findCyclesForCycleTime(ekId, new Timestamp(startTime.getMillis()), cycle.getReadingTimestamp());
                        Float coldWater = 0f;
                        for (Cycle ekCycle : ekCycles) {
                            coldWater += ekCycle.getColdWaterVolume();
                        }
                        total = coldWater;
                        row.createCell(3).setCellValue(total);
                    }
                    else {
                        Float cold = cycle.getColdWaterVolume()!=null?cycle.getColdWaterVolume():0f;
                        total = cold;
                        row.createCell(3).setCellValue(formatDecimal(total));
                    }
                    float variance = comparison - total;
                    float manufacturerVariance = 0f;
                    varianceTotal += variance;
                    if (manufacturer) {
                        manufacturerVariance = manufacturerComparison - total;
                        manufacturerVarianceTotal += manufacturerVariance;
                        row.createCell(6).setCellValue(formatDecimal(variance));
                    }
                    else {
                        row.createCell(5).setCellValue(formatDecimal(variance));
                    }
                    FormulaVariance formulaVariance = findFormulaVariance(classificationName);
                    if (formulaVariance == null) {
                        formulaVariance = new FormulaVariance(variance, manufacturerVariance, 1);
                    }
                    else {
                        formulaVariance.setDayCount(formulaVariance.getDayCount()+1);
                        formulaVariance.setVarianceTotal(formulaVariance.getVarianceTotal()+variance);
                        formulaVariance.setManufacturerVarianceTotal(formulaVariance.getManufacturerVarianceTotal() + manufacturerVariance);
                    }
                    formulaVarianceList.put(classificationName, formulaVariance);

                    Float percentageDiff = calculatePercentageDiff(comparison, total);

                    if (manufacturer) {
                        Float manPercentageDiff = calculatePercentageDiff(manufacturerComparison, total);
                        row.createCell(7).setCellValue(formatPercentage(percentageDiff));
                        row.createCell(9).setCellValue(formatPercentage(manPercentageDiff));
                        if (Math.abs(manPercentageDiff) > .05f) {
                            row.getCell(8).setCellStyle(highlightStyle);
                            row.getCell(9).setCellStyle(highlightStyle);
                        }
                        if (Math.abs(percentageDiff) > .05f) {
                            row.getCell(6).setCellStyle(highlightStyle);
                            row.getCell(7).setCellStyle(highlightStyle);
                        }
                    }
                    else {
                        row.createCell(6).setCellValue(formatPercentage(percentageDiff));
                        if (Math.abs(percentageDiff) > .05f) {
                            row.getCell(5).setCellStyle(highlightStyle);
                            row.getCell(6).setCellStyle(highlightStyle);
                        }
                    }
                }
                for (Entry<String, FormulaVariance> entry : formulaVarianceList.entrySet()) {
                    HSSFRow row = summarySheet.createRow(summarySheet.getLastRowNum()+1);
                    FormulaVariance variance = entry.getValue();
                    Float total = variance.getVarianceTotal();
                    int days = variance.getDayCount();

                    double averageVariance = formatDecimal(total / days);
                    double yearlyVariance = formatDecimal((float)(averageVariance*365));
                    double monthlyVariance = formatDecimal((float)(averageVariance*365/12));
                    double monthlySavings = formatDecimal((float)(averageVariance*365/12*costPerGallon));

                    row.createCell(0).setCellValue(entry.getKey());
                    row.createCell(1).setCellValue(averageVariance);
                    row.createCell(2).setCellValue(yearlyVariance);
                    row.createCell(3).setCellValue(monthlyVariance);
                    row.createCell(4).setCellValue(monthlySavings);
                    row.getCell(4).setCellStyle(moneyStyle);

                    if (manufacturer) {
                        Float manTotal = variance.getManufacturerVarianceTotal();
                        double manAverageVar = formatDecimal(manTotal / days);
                        double manYearVar = formatDecimal((float)(manAverageVar*365));
                        double manMonthVar = formatDecimal((float)(manAverageVar*365/12));
                        double manSavings = formatDecimal((float)(manAverageVar*365/12*costPerGallon));

                        row.createCell(5).setCellValue(manAverageVar);
                        row.createCell(6).setCellValue(manYearVar);
                        row.createCell(7).setCellValue(manMonthVar);
                        row.createCell(8).setCellValue(manSavings);
                        row.getCell(8).setCellStyle(moneyStyle);
                    }
                }
            }
            sheet = null;

            HSSFRow lastRow = summarySheet.getRow(summarySheet.getLastRowNum());
            for (Cell cell : lastRow) {
                cell.setCellStyle(underlineStyle);
            }
            summarySheet.createRow(summarySheet.getLastRowNum()+1);
            formulaVarianceList.clear();
            lastCheck = null;
        }
        for (int z=0; z < summarySheet.getRow(0).getLastCellNum(); z++) {
            summarySheet.autoSizeColumn(z);
        }

        return writeFile(workbook, "compareReport.xls");
    }

    private CellStyle getMoneyStyle(HSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat((short) 7);
        return style;
    }
    private CellStyle getUnderlineStyle(HSSFWorkbook workbook) {
        CellStyle underline = workbook.createCellStyle();
        underline.setBorderBottom(CellStyle.BORDER_MEDIUM);
        return underline;
    }

    private boolean checkDailySeperator(HSSFRow row, DateTime readingDateTime, CellStyle style) {
        boolean result = false;
        HSSFSheet sheet = row.getSheet();
        int rowNum = row.getRowNum();
        HSSFRow lastRow = sheet.getRow(rowNum - 1);

        if (lastCheck == null) {
            lastCheck = readingDateTime;
            return result;
        }
        else if (!lastCheck.dayOfWeek().equals(readingDateTime.dayOfWeek())) {
            result = true;
            if (row.getRowNum() > 1) {
                for (Cell cell : lastRow) {
                    cell.setCellStyle(style);
                }
            }
        }
        lastCheck = readingDateTime;
        return result;
    }

    private Float getComparisonValue(Cycle cycle) {
        Machine machine;
        Classification classification;
        XerosLocalStaticValue xlsv = null;
        LocalStaticValue lsv = null;
        Float cold;
        Float hot;
        Float result = 0f;

        if (cycle != null) {
            machine = cycle.getMachine();
            classification = cycle.getClassification();

            if (classification == null) return result;

            if (machine.getManufacturer().equalsIgnoreCase("xeros")) {
                xlsv = xlsvRepository.findByClassification(classification.getId());
                if (xlsv != null) {
                    cold = xlsv.getColdWater();
                    hot = xlsv.getHotWater();
                    result = cold + hot;
                }
            }
            else {
                lsv = lsvRepository.findByClassification(classification.getId());
                if (lsv != null) {
                    cold = lsv.getColdWater();
                    hot = lsv.getHotWater();
                    result = cold + hot;
                }
            }
        }
        return result;
    }
    private Float getManufacturerComparisonValue(Cycle cycle) {
        Machine machine;
        Classification classification;
        XerosLocalStaticValue xlsv = null;
        LocalStaticValue lsv = null;
        Float cold;
        Float hot;
        Float result = 0f;

        if (cycle != null) {
            machine = cycle.getMachine();
            classification = cycle.getClassification();

            if (classification == null) return result;

            if (machine.getManufacturer().equalsIgnoreCase("xeros")) {
                xlsv = xlsvRepository.findByClassification(classification.getId());
                if (xlsv != null) {
                    cold = xlsv.getManufacturerColdWater()!=null?xlsv.getManufacturerColdWater():0f;
                    hot = xlsv.getManufacturerHotWater()!=null?xlsv.getManufacturerHotWater():0f;
                    result = cold + hot;
                }
            }
            else {
                lsv = lsvRepository.findByClassification(classification.getId());
                if (lsv != null) {
                    cold = lsv.getManufacturerColdWater()!=null?lsv.getManufacturerColdWater():0f;
                    hot = lsv.getManufacturerHotWater()!=null?lsv.getManufacturerHotWater():0f;
                    result = cold + hot;
                }
            }
        }
        return result;
    }

    private double formatDecimal(Float runTime) {
        double result = 0;
        if (runTime != null) {
            result = Double.valueOf(decimalFormat.format(runTime));
        }
        return result;
    }
    private String formatPercentage(Float percent) {
        String result = "";
        if (percent != null) {
            if (percent > 1) {
                percent = 1f;
            }
            else if (percent < -1) {
                percent = -1f;
            }
            result = percentageFormat.format(percent);
        }
        return result;
    }

    private String describeExceptions(String exception) {
        StringBuilder buff = new StringBuilder();
        String seperator = "";
        List<String> strings = new ArrayList<String>();

        if (exception != null) {
            for (int i=0; i<exception.length(); i++) {
                char c = exception.charAt(i);
                switch (c) {
                    case '1':
                        strings.add("highcold");
                        break;
                    case '2':
                        strings.add("lowcold");
                        break;
                    case '3':
                        strings.add("highhot");
                        break;
                    case '4':
                        strings.add("lowhot");
                        break;
                    case '5':
                        strings.add("hightime");
                        break;
                    case '6':
                        strings.add("lowtime");
                        break;
                    case '7':
                        strings.add("watermin");
                        break;
                }
            }
        }
        for (String string : strings) {
            buff.append(seperator);
            buff.append(string);
            seperator = ",";
        }
        return buff.toString();
    }


    private List<Cycle> processException(String startDate, String endDate, Machine machine, Integer exceptionType, Integer unknownId) {
        List<Cycle> result = new ArrayList<Cycle>();
        if (startDate == null) {
            startDate = "2000-01-01";
        }
        if (endDate == null) {
            endDate = "2999-12-31";
        }
        Timestamp start = Timestamp.valueOf(startDate + " " + "00:00:00");
        Timestamp end = Timestamp.valueOf(endDate + " " + "23:59:59");
        Integer id = machine.getId();
        String unknown = "";
        Collection<DaiMeterActual> actuals = null;


        switch (exceptionType) {
            //Returns Cycles with no exceptions, or "null" exceptions (haven't been processed)
            case 0: {
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionLikeOrNull(id, start, end, "");
                break;
            }
            case 1: {
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[1]");
                break;
            }
            case 2: {
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[2]");
                break;
            }
            case 3: {
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[3]");
                break;
            }
            case 4: {
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[4]");
                break;
            }
            case 5: {
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[5]");
                break;
            }
            case 6: {
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[6]");
                break;
            }
            case 7: {
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[7]");
                break;
            }
            case -1: {
                //returns all types of cycles, with or without exceptions, or unprocessed
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetween(id, start, end);
                break;
            }
            case -2: {
                //returns only cycles with exceptions
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[0-9]");
                break;
            }
            case 10: {
                //returns only cycles with cold water exceptions
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[12]");
                break;
            }
            case 20: {
                //all hot water
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[34]");
                break;
            }
            case 30: {
                //all runtime
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[56]");
                break;
            }
            case 40: {
                //all low water
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[24]");
                break;
            }
            case 50: {
                //all high water
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[13]");
                break;
            }
            case 60: {
                //all water
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(id, start, end, "[1234]");
                break;
            }
            case 100: {
                //null exceptions
                actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionIsNull(id, start, end);
            }
        }
        if (actuals != null && !actuals.isEmpty()) {
            switch (unknownId) {
                case 0: {
                    //all cycles
                    result = cycleRepository.findByDaiMeterActualIn(actuals);
                    break;
                }
                case 1: {
                    //no unknowns
                    unknown = "^[^1]";
                    result = cycleRepository.findByDaiMeterActualInAndClassificationRegexp(actuals, unknown);
                    break;
                }
                case 2: {
                    //only unknowns
                    unknown = "^1$";
                    result = cycleRepository.findByDaiMeterActualInAndClassificationRegexp(actuals, unknown);
                    break;
                }
                default: {
                    result = cycleRepository.findByDaiMeterActualIn(actuals);
                    break;
                }
            }
        }

        return result;
    }

    //    @Scheduled(cron = "* * */2 * * *")
    public void checkStatusTime()  {
        List<Integer> machineIds = machineRepository.findAllMachineIds();
        List<Status> statusList = getStatus(machineIds);

        for (Status status : statusList) {
            if (status != null && status.getTimestamp().before(getTimestampForIdleInterval())) {
                Machine machine =  machineRepository.findById(status.getMachineId());
                createStatus(status.getDaiIdentifier(), machine, -2);
            }
        }
    }

    @Transactional
    public Status createStatus(String daiIdentifier, Machine machine, int statusCode) {
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

    private void initializeLogSheet(HSSFSheet sheet, CellStyle style) {
        HSSFRow header = sheet.createRow(0);

        header.createCell(0).setCellValue("Company Name");
        header.createCell(1).setCellValue("Location Name");
        header.createCell(2).setCellValue("Machine Name");
        header.createCell(3).setCellValue("Last Cycle");
        header.createCell(4).setCellValue("Last Status");

        for (int i = 0; i < header.getLastCellNum(); i++) {
            header.getCell(i).setCellStyle(style);
        }
    }
    private void initializeSheet(Sheet sheet, CellStyle style) {
        Row rowHeader = sheet.createRow(0);

        rowHeader.createCell(0).setCellValue("Company Name");
        rowHeader.createCell(1).setCellValue("Location Name");
        rowHeader.createCell(2).setCellValue("Machine Name");
        rowHeader.createCell(3).setCellValue("Classification Name");
        rowHeader.createCell(4).setCellValue("Reading Date");
        rowHeader.createCell(5).setCellValue("Cycle Start Time");
        rowHeader.createCell(6).setCellValue("Cycle End Time");
        rowHeader.createCell(7).setCellValue("Total Water");
        rowHeader.createCell(8).setCellValue("Cold Water");
        rowHeader.createCell(9).setCellValue("Hot Water");
        rowHeader.createCell(10).setCellValue("Therms");
        rowHeader.createCell(11).setCellValue("Run Time");
        rowHeader.createCell(12).setCellValue("Exception Codes");
        rowHeader.createCell(13).setCellValue("Exception Description");
        rowHeader.createCell(14).setCellValue("Intended Formula");

        for (Cell cell: rowHeader) {
            cell.setCellStyle(style);
        }
        for (int j=0; j < sheet.getRow(sheet.getLastRowNum()).getLastCellNum(); j++) {
            sheet.autoSizeColumn(j);
        }
    }
    private void initializeEkSheet(Sheet sheet, CellStyle style) {
        Row rowHeader = sheet.createRow(0);

        rowHeader.createCell(0).setCellValue("Company Name");
        rowHeader.createCell(1).setCellValue("Location Name");
        rowHeader.createCell(2).setCellValue("Machine Name");
        rowHeader.createCell(3).setCellValue("Classification Name");
        rowHeader.createCell(4).setCellValue("Reading Date");
        rowHeader.createCell(5).setCellValue("Cycle Start Time");
        rowHeader.createCell(6).setCellValue("Cycle End Time");
        rowHeader.createCell(7).setCellValue("Total Water");
        rowHeader.createCell(8).setCellValue("Cold Water");
        rowHeader.createCell(9).setCellValue("Hot Water");
        rowHeader.createCell(10).setCellValue("Therms");
        rowHeader.createCell(11).setCellValue("Run Time");
        rowHeader.createCell(12).setCellValue("Exception Codes");
        rowHeader.createCell(13).setCellValue("Exception Description");
        rowHeader.createCell(14).setCellValue("Intended Formula");
        rowHeader.createCell(15).setCellValue("EK Records");

        for (Cell cell: rowHeader) {
            cell.setCellStyle(style);
        }
        for (int j=0; j < sheet.getRow(sheet.getLastRowNum()).getLastCellNum(); j++) {
            sheet.autoSizeColumn(j);
        }
    }
    private void initializeStatusGapSheet(HSSFSheet sheet, CellStyle style) {
        HSSFRow header = sheet.createRow(0);

        header.createCell(0).setCellValue("Company Name");
        header.createCell(1).setCellValue("Location Name");
        header.createCell(2).setCellValue("Machine Name");
        header.createCell(3).setCellValue("Disconnected");
        header.createCell(4).setCellValue("Reconnected");
        header.createCell(5).setCellValue("Gap Length");

        for (int i=0; i<header.getLastCellNum(); i++) {
            header.getCell(i).setCellStyle(style);
        }
    }
    private void initializeCompareSheet(HSSFSheet sheet, CellStyle style) {
        HSSFRow header = sheet.createRow(0);

        header.createCell(0).setCellValue("Date");
        header.createCell(1).setCellValue("Formula");
        header.createCell(2).setCellValue("Start Time");
        header.createCell(3).setCellValue("Total Water");
        header.createCell(4).setCellValue("Expected Water");
        header.createCell(5).setCellValue("Variance");
        header.createCell(6).setCellValue("Percentage Variance");
        header.createCell(7).setCellValue("Intended Formula");
        header.createCell(8).setCellValue("Daily Variance");
        header.createCell(9).setCellValue("Yearly Variance");
        header.createCell(10).setCellValue("Monthly Variance");
        header.createCell(11).setCellValue("Monthly Savings Not Captured");

        for (Cell cell : header) {
            cell.setCellStyle(style);
        }
    }
    private void initializeCompareSheetManufacturer(HSSFSheet sheet, CellStyle style) {
        HSSFRow header = sheet.createRow(0);

        header.createCell(0).setCellValue("Date");
        header.createCell(1).setCellValue("Formula");
        header.createCell(2).setCellValue("Start Time");
        header.createCell(3).setCellValue("Total Water");
        header.createCell(4).setCellValue("Expected Water");
        header.createCell(5).setCellValue("Manufacturer Expected Water");
        header.createCell(6).setCellValue("Variance");
        header.createCell(7).setCellValue("Percentage Variance");
        header.createCell(8).setCellValue("Manufacturer Variance");
        header.createCell(9).setCellValue("Percentage Manufacturer Variance");
        header.createCell(10).setCellValue("Intended Formula");
        header.createCell(11).setCellValue("Daily Variance");
        header.createCell(12).setCellValue("Yearly Variance");
        header.createCell(13).setCellValue("Monthly Variance");
        header.createCell(14).setCellValue("Monthly Savings Not Captured");
        header.createCell(15).setCellValue("Manufacturer Daily Variance");
        header.createCell(16).setCellValue("Manufacturer Yearly Variance");
        header.createCell(17).setCellValue("Manufacturer Monthly Variance");
        header.createCell(18).setCellValue("Manufacturer Monthly Savings Not Captured");

        for (Cell cell : header) {
            cell.setCellStyle(style);
        }
    }
    private void initializeSummarySheet(HSSFSheet sheet, CellStyle style) {
        HSSFRow header = sheet.createRow(0);

        header.createCell(0).setCellValue("Formula");
        header.createCell(1).setCellValue("Average Variance Per Day");
        header.createCell(2).setCellValue("Average Variance Per Year");
        header.createCell(3).setCellValue("Average Variance Per Month");
        header.createCell(4).setCellValue("Average Monthly Savings Not Tracked");

        for (int i=0; i<header.getLastCellNum(); i++) {
            header.getCell(i).setCellStyle(style);
        }
    }
    private void initializeSummarySheetManufacturer(HSSFSheet sheet, CellStyle style) {
        HSSFRow header = sheet.createRow(0);

        header.createCell(0).setCellValue("Formula");
        header.createCell(1).setCellValue("Average Variance Per Day");
        header.createCell(2).setCellValue("Average Variance Per Year");
        header.createCell(3).setCellValue("Average Variance Per Month");
        header.createCell(4).setCellValue("Average Monthly Savings Not Tracked");
        header.createCell(5).setCellValue("Average Manufacturer Variance Per Day");
        header.createCell(6).setCellValue("Average Manufacturer Variance Per Year");
        header.createCell(7).setCellValue("Average Manufacturer Variance Per Month");
        header.createCell(8).setCellValue("Average Manufacturer Monthly Savings Not Tracked");

        for (int i=0; i<header.getLastCellNum(); i++) {
            header.getCell(i).setCellStyle(style);
        }

    }


    private CellStyle getHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setBorderBottom(CellStyle.BORDER_MEDIUM);
        style.setBorderLeft(CellStyle.BORDER_MEDIUM);
        style.setBorderRight(CellStyle.BORDER_MEDIUM);
        style.setBorderTop(CellStyle.BORDER_MEDIUM);
        style.setFont(font);
        style.setFillForegroundColor(HSSFColor.GREY_40_PERCENT.index);
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        return style;
    }
    private CellStyle getVarianceHighlightStyle(HSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(HSSFColor.LIGHT_YELLOW.index);
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        return style;
    }
    private CellStyle getRedHighlightStyle(HSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(HSSFColor.RED.index);
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        return style;
    }
    private String formatTimeDiffString(long diff) {
        if (TimeUnit.SECONDS.toDays(diff) < 1) {
            return String.format("%02d hours, %02d minutes, %02d seconds", TimeUnit.SECONDS.toHours(diff),
                    TimeUnit.SECONDS.toMinutes(diff) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.SECONDS.toSeconds(diff) % TimeUnit.MINUTES.toSeconds(1));
        }
        else {
            return String.format("%02d days, %02d hours, %02d minutes, %02d seconds", TimeUnit.SECONDS.toDays(diff),
                    TimeUnit.SECONDS.toHours(diff) % TimeUnit.DAYS.toHours(1),
                    TimeUnit.SECONDS.toMinutes(diff) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.SECONDS.toSeconds(diff) % TimeUnit.MINUTES.toSeconds(1));
        }
    }
    private File writeFile(Workbook workbook, String filename) {
        if (filename == null) {
            filename = "output.xls";
        }
        File file = new File(filename);
        try {
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            workbook.close();
            out.close();
        } catch (FileNotFoundException e) {
            //handle
        } catch (IOException ex) {
            //handle
        }
        return file;
    }

    private long calculateTimeLong(Status status) {
        LocalDateTime ldt = new LocalDateTime(status.getTimestamp().getTime());
        DateTime time = ldt.toDateTime(DateTimeZone.UTC);
        return time.getMillis() / 1000;
    }

    private Timestamp getTimestampForLastLogRedInterval() {
        Calendar c = Calendar.getInstance();
        //default is 72 hours
        int interval = 72;
        try {
            StaticValue staticValue = staticValueRepository.findByName("last_log_red");
            if (staticValue != null) {
                interval = Integer.parseInt(staticValue.getValue());
            }
        } catch (NumberFormatException e) {
            String msg = "unable to convert interval to int, no last_log_red interval in db?";
            logger.warn(msg, e);
        }
        c.add(Calendar.HOUR, -interval);
        return new Timestamp(c.getTimeInMillis());
    }
    private Timestamp getTimestampForLastLogYellowInterval() {
        Calendar c = Calendar.getInstance();
        //default is 36 hours
        int interval = 36;
        try {
            StaticValue staticValue = staticValueRepository.findByName("last_log_yellow");
            if (staticValue != null) {
                interval = Integer.parseInt(staticValue.getValue());
            }
        } catch (NumberFormatException e) {
            String msg = "unable to convert interval to int, no last_log_red interval in db?";
            logger.warn(msg, e);
        }
        c.add(Calendar.HOUR, -interval);
        return new Timestamp(c.getTimeInMillis());
    }
    private Timestamp getTimestampForHeartbeatRedInterval() {
        Calendar c = Calendar.getInstance();
        //default is 120 minutes
        int interval = 120;
        try {
            StaticValue staticValue = staticValueRepository.findByName("heartbeat_red");
            if (staticValue != null) {
                interval = Integer.parseInt(staticValue.getValue());
            }
        } catch (NumberFormatException e) {
            String msg = "unable to convert interval to int, no heartbeat_red interval in db?";
            logger.warn(msg, e);
        }
        c.add(Calendar.MINUTE, -interval);
        return new Timestamp(c.getTimeInMillis());
    }
    private Timestamp getTimestampForHeartbeatYellowInterval() {
        Calendar c = Calendar.getInstance();
        //default is 30 minutes
        int interval = 30;
        try {
            StaticValue staticValue = staticValueRepository.findByName("heartbeat_yellow");
            if (staticValue != null) {
                interval = Integer.parseInt(staticValue.getValue());
            }
        } catch (NumberFormatException e) {
            String msg = "unable to convert interval to int, no heartbeat_red interval in db?";
            logger.warn(msg, e);
        }
        c.add(Calendar.MINUTE, -interval);
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
    private Float calculatePercentageDiff(Float benchmarkValue, Float actualValue) {
        Float change = actualValue - benchmarkValue;
        Float sum = actualValue + benchmarkValue;
        if (sum == 0f) {
            return 0f;
        }
        return change / (sum / 2);
    }


    //no longer used
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
            List<Status> list = statusRepository.findHistoryByMachineIdWithLimit(id, 100);
            statusList.addAll(statusRepository.findHistoryByMachineIdWithLimit(id, 100));
        }
        return statusList;
    }
    public boolean receivePing(String daiIdentifier) {
        List<ActiveDai> daiList = activeDaiRepository.findByDaiIdentifier(daiIdentifier);
        if (daiList != null && daiList.size() > 0) {
            ActiveDai dai = daiList.iterator().next();
            dai.setLastPing(new Timestamp(System.currentTimeMillis()));
            return true;
        }
        return false;
    }
}

