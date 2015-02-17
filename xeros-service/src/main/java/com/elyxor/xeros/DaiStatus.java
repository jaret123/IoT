package com.elyxor.xeros;

import com.elyxor.xeros.model.*;
import com.elyxor.xeros.model.repository.*;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.sql.DataSource;
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
import java.util.*;
import java.util.Map.Entry;
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
    @Autowired LocationRepository locationRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired LocalStaticValueRepository lsvRepository;
    @Autowired XerosLocalStaticValueRepository xlsvRepository;

    private static Logger logger = LoggerFactory.getLogger(DaiStatus.class);


    private static final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final DateTimeFormatter dateAndTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("HH:mm:ss");
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private DateTime lastCheck = null; //used to determine row separator for compare report
    private static Map<String, FormulaVariance> formulaVarianceList = new LinkedHashMap<String, FormulaVariance>();

    public class FormulaVariance {
        float varianceTotal;
        int dayCount;

        public FormulaVariance(float varianceTotal, int dayCount) {
            this.varianceTotal = varianceTotal;
            this.dayCount = dayCount;
        }

        public float getVarianceTotal() {
            return varianceTotal;
        }
        public void setVarianceTotal(float varianceTotal) {
            this.varianceTotal = varianceTotal;
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
    public File getStatusGaps() {
        List<Status> statusList = calculateStatusGaps(new ArrayList<Machine>());
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

    public List<Status> calculateStatusGaps(List<Machine> machineList){
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
                long currentLong = calculateTimeLong(current);
                long prevLong = calculateTimeLong(prev);
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

        Integer exceptionId = 0;

        if (to == null) {
            to = from;
        }
        if (exception != null) {
            exceptionId = Integer.parseInt(exception);
        }
        if (type != null && type.equals("compare")){
            if (company != null) {
                Integer companyId = Integer.parseInt(company);
                return getCompareReportsForCompany(from, to, exceptionId, companyId);
            }
            else if (location != null) {
                Integer locationId = Integer.parseInt(location);
                return getCompareReportsForLocation(from, to, exceptionId, locationId);
            }
            else if (machine != null) {
                Integer machineId = Integer.parseInt(machine);
                return getCompareReportsForMachine(from, to, exceptionId, machineId);
            }
            else {
                return getCompareReports(from, to, exceptionId);
            }
        }
        if (type != null && type.equals("ek")) {
            if (company != null) {
                Integer companyId = Integer.parseInt(company);
                return getEkCycleReportsForCompany(from, to, exceptionId, companyId);
            }
            else if (location != null) {
                Integer locationId = Integer.parseInt(location);
                return getEkCycleReportsForLocation(from, to, exceptionId, locationId);
            }
            else if (machine != null) {
                Integer machineId = Integer.parseInt(machine);
                return getEkCycleReportsForMachine(from, to, exceptionId, machineId);
            }
            else {
                return getEkCycleReports(from, to, exceptionId);
            }
        }
        else {
            if (company != null) {
                Integer companyId = Integer.parseInt(company);
                return getCycleReportsForCompany(from, to, exceptionId, companyId);
            }
            else if (location != null) {
                Integer locationId = Integer.parseInt(location);
                return getCycleReportsForLocation(from, to, exceptionId, locationId);
            }
            else if (machine != null) {
                Integer machineId = Integer.parseInt(machine);
                return getCycleReportsForMachine(from, to, exceptionId, machineId);
            }
            else {
                return getCycleReports(from, to, exceptionId);
            }
        }
    }

    private File getCompareReportsForMachine(String from, String to, Integer exceptionId, Integer machineId) {
        List<Machine> machineList = new ArrayList<Machine>();
        machineList.add(machineRepository.findOne(machineId));
        return createCompareReports(machineList, from, to, exceptionId);
    }
    private File getCompareReportsForCompany(String from, String to, Integer exceptionId, Integer companyId) {
        Iterable<Location> locations = (companyRepository.findOne(companyId)).getLocations();
        List<Machine> machineList = new ArrayList<Machine>();
        for (Location location : locations) {
            machineList.addAll(location.getMachines());
        }
        return createCompareReports(machineList, from, to, exceptionId);
    }
    private File getCompareReportsForLocation(String from, String to, Integer exceptionId, Integer locationId) {
        Location location = locationRepository.findOne(locationId);
        Iterable<Machine> machineList = location.getMachines();
        return createCompareReports(machineList, from, to, exceptionId);
    }
    private File getCompareReports(String from, String to, Integer exceptionId) {
        return createCompareReports(machineRepository.findAll(), from, to, exceptionId);
    }

    public File createEkCycleReports(Iterable<MachineMapping> machineList, String startDate, String endDate, Integer exceptionType) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        Sheet sheet = null;

        for (MachineMapping machineMapping : machineList) {
            Machine machine = machineRepository.findById(machineMapping.getDaiId());
            List<Cycle> cycleList = processException(startDate, endDate, machine, exceptionType);
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
                        initializeSheet(sheet);
                    }

                    DateTime readingDateTime = new DateTime(cycle.getReadingTimestamp());
                    DateTime startTime = readingDateTime.minusMinutes(Math.round(cycle.getRunTime()));

                    String readingDate = readingDateTime.toString(dateFormatter);
                    String cycleEndTime = readingDateTime.toString(dateAndTimeFormatter);
                    String cycleStartTime = startTime.toString(dateAndTimeFormatter);

                    String exception = "";
                    String className = "";
                    Classification classification;
                    DaiMeterActual actual;

                    if ((classification = cycle.getClassification())!=null){
                        className = classification.getName();
                    }
                    if ((actual = cycle.getDaiMeterActual())!=null) {
                        exception = actual.getException();
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


                    List<Cycle> ekCycles = cycleRepository.findCyclesForCycleTime(machineMapping.getEkId(), new Timestamp(startTime.getMillis()), cycle.getReadingTimestamp());
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
    public File createCycleReports(Iterable<Machine> machineList, String startDate, String endDate, Integer exceptionType) {
        HSSFWorkbook workbook = new HSSFWorkbook();
        FileOutputStream out = null;
        File file = null;
        HSSFSheet sheet = null;

        for (Machine machine : machineList) {
            List<Cycle> cycleList = processException(startDate, endDate, machine, exceptionType);
            if (cycleList != null) {
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

                for (int i = 0; i < cycleList.size(); i++) {
                    Cycle cycle = cycleList.get(i);
                    if (sheet == null) {
                        sheet = workbook.createSheet(companyName.substring(0,3) + " - " + locationName.substring(0,3) + " - " + machineName);
                        initializeSheet(sheet);
                    }

                    DateTime readingDateTime = new DateTime(cycle.getReadingTimestamp());
                    DateTime startTime = readingDateTime.minusMinutes(Math.round(cycle.getRunTime()));

                    String readingDate = readingDateTime.toString(dateFormatter);
                    String cycleEndTime = readingDateTime.toString(dateAndTimeFormatter);
                    String cycleStartTime = startTime.toString(dateAndTimeFormatter);

                    String exception = "";
                    String className = "";
                    Classification classification;
                    DaiMeterActual actual;

                    if ((classification = cycle.getClassification())!=null){
                        className = classification.getName();
                    }
                    if ((actual = cycle.getDaiMeterActual())!=null) {
                        exception = actual.getException();
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

                    Float total = cycle.getColdWaterVolume();
                    Float hot = cycle.getHotWaterVolume();
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
//        try {
//            file = new File("output.xls");
//            out = new FileOutputStream(file);
//            workbook.write(out);
//            out.close();
//            return file;
//        } catch (FileNotFoundException fnfe) {
//            //handle
//        } catch (IOException ioe) {
//            //handle
//        }
//        return file;
    }
    public File createCompareReports(Iterable<Machine> machines, String start, String end, Integer exceptionId) {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = null;
        MachineMapping machineMapping = null;
        Integer ekId = 0;

        HSSFSheet summarySheet = workbook.createSheet("Summary");
        initializeSummarySheet(summarySheet);

        CellStyle headerStyle = getHeaderStyle(workbook);
        CellStyle underlineStyle = getUnderlineStyle(workbook);
        CellStyle moneyStyle = getMoneyStyle(workbook);
        CellStyle highlightStyle = getVarianceHighlightStyle(workbook);

        for (Machine machine : machines) {
            if ((machineMapping = machineMappingRepository.findOne(machine.getId())) != null) {
                ekId = machineMapping.getEkId();
            }

            List<Cycle> cycleList = processException(start, end, machine, exceptionId);
            if (cycleList != null) {
                Location location = machine.getLocation();
                String companyName = location.getCompany().getName();
                String machineName = machine.getName();
                String locationName = location.getName();

                HSSFRow machineNameRow = summarySheet.createRow(summarySheet.getLastRowNum()+1);
                machineNameRow.createCell(0).setCellValue(locationName);
                machineNameRow.createCell(1).setCellValue(machineName);
                for (Cell cell : machineNameRow)
                    cell.setCellStyle(headerStyle);

                float varianceTotal = 0;

                for (Cycle cycle: cycleList) {
                    if (sheet == null) {
                        sheet = workbook.createSheet(companyName.substring(0,3) + " - " + locationName.substring(0,3) + " - " + machineName);
                        initializeCompareSheet(sheet);
                        for (int j=0; j < sheet.getRow(0).getLastCellNum(); j++) {
                                sheet.autoSizeColumn(j);
                        }

                    }

                    DateTime readingDateTime = new DateTime(cycle.getReadingTimestamp());
                    DateTime startTime = readingDateTime.minusMinutes(Math.round(cycle.getRunTime()));

                    String readingDate = readingDateTime.toString(dateFormatter);
                    String cycleEndTime = readingDateTime.toString(dateAndTimeFormatter);
                    String cycleStartTime = startTime.toString(timeFormatter);

                    String classification = cycle.getClassification().getName();

                    Float total = 0f;
                    Float comparison = getComparisonValue(cycle);

                    HSSFRow row = sheet.createRow(sheet.getLastRowNum()+1);

                    if (checkDailySeperator(row, readingDateTime, underlineStyle)) {
                        for (int k=0; k < 7; k++)
                            row.createCell(k);
                        row.createCell(7).setCellValue(varianceTotal);
                        row.createCell(8).setCellValue(varianceTotal*365);
                        row.createCell(9).setCellValue(formatDecimal((float)(varianceTotal*365/12)));
                        row.createCell(10).setCellValue(formatDecimal((float)(varianceTotal*365/12*.00897)));
                        row.getCell(10).setCellStyle(moneyStyle);
                        varianceTotal = 0f;
                        row = sheet.createRow(sheet.getLastRowNum()+1);
                    }

                    row.createCell(0).setCellValue(readingDate);
                    row.createCell(1).setCellValue(classification);
                    row.createCell(2).setCellValue(cycleStartTime);
                    row.createCell(4).setCellValue(comparison);

                    if (machineMapping != null) {
                        List<Cycle> ekCycles = cycleRepository.findCyclesForCycleTime(machineMapping.getEkId(), new Timestamp(startTime.getMillis()), cycle.getReadingTimestamp());
                        Float coldWater = 0f;
                        Float hotWater = 0f;
                        for (Cycle ekCycle : ekCycles) {
                            coldWater += ekCycle.getColdWaterVolume();
                            hotWater += ekCycle.getHotWaterVolume();
                        }
                        total = coldWater + hotWater;
                        row.createCell(3).setCellValue(total);
                    }
                    else {
                        Float cold = cycle.getColdWaterVolume()!=null?cycle.getColdWaterVolume():0f;
                        Float hot = cycle.getHotWaterVolume()!=null?cycle.getHotWaterVolume():0f;
                        total = hot + cold;
                        row.createCell(3).setCellValue(formatDecimal(total));
                    }
                    float variance = comparison - total;
                    varianceTotal += variance;
                    row.createCell(5).setCellValue(formatDecimal(variance));

                    FormulaVariance formulaVariance = findFormulaVariance(classification);
                    if (formulaVariance == null) {
                        formulaVariance = new FormulaVariance(variance, 1);
                    }
                    else {
                        formulaVariance.setDayCount(formulaVariance.getDayCount()+1);
                        formulaVariance.setVarianceTotal(formulaVariance.getVarianceTotal()+variance);
                    }
                    formulaVarianceList.put(classification, formulaVariance);

                    Float percentageDiff = calculatePercentageDiff(comparison, total);
                    row.createCell(6).setCellValue(percentageDiff);

                    if (Math.abs(percentageDiff) > .05f) {
                        row.getCell(5).setCellStyle(highlightStyle);
                        row.getCell(6).setCellStyle(highlightStyle);
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
                    double monthlySavings = formatDecimal((float)(averageVariance*365/12*.00879));

                    row.createCell(0).setCellValue(entry.getKey());
                    row.createCell(1).setCellValue(averageVariance);
                    row.createCell(2).setCellValue(yearlyVariance);
                    row.createCell(3).setCellValue(monthlyVariance);
                    row.createCell(4).setCellValue(monthlySavings);
                    row.getCell(4).setCellStyle(moneyStyle);
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
        style.setDataFormat((short)7);
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
        HSSFWorkbook workbook = sheet.getWorkbook();
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

            if (machine.getManufacturer().equalsIgnoreCase("xeros")) {
                xlsv = xlsvRepository.findByClassification(classification);
                if (xlsv != null) {
                    cold = xlsv.getColdWater();
                    hot = xlsv.getHotWater();
                    result = cold + hot;
                }
            }
            else {
                lsv = lsvRepository.findByClassification(classification);
                if (lsv != null) {
                    cold = lsv.getColdWater();
                    hot = lsv.getHotWater();
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

    private File getCycleReports(String startDate, String endDate, Integer exceptionType) {
        Iterable<Machine> machineList = machineRepository.findAll();
        return createCycleReports(machineList, startDate, endDate, exceptionType);
    }
    private File getCycleReportsForMachine(String from, String to, Integer exception, Integer machine) {
        List<Machine> machineList = new ArrayList<Machine>();
        machineList.add(machineRepository.findOne(machine));
        return createCycleReports(machineList, from, to, exception);
    }
    private File getCycleReportsForLocation(String fromDate, String toDate, Integer exceptionId, Integer locationId) {
        Location location = locationRepository.findOne(locationId);
        Iterable<Machine> machineList = location.getMachines();
        return createCycleReports(machineList, fromDate, toDate, exceptionId);
    }
    private File getCycleReportsForCompany(String fromDate, String toDate, Integer exceptionId, Integer companyId) {
        Iterable<Location> locations = (companyRepository.findOne(companyId)).getLocations();
        List<Machine> machineList = new ArrayList<Machine>();
        for (Location location : locations) {
            machineList.addAll(location.getMachines());
        }
        return createCycleReports(machineList, fromDate, toDate, exceptionId);
    }

    private File getEkCycleReports(String startDate, String endDate, Integer exceptionType) {
        return createEkCycleReports(machineMappingRepository.findAll(), startDate, endDate, exceptionType);
    }
    private File getEkCycleReportsForMachine(String fromDate, String toDate, Integer exception, Integer machineId) {
        Iterable<MachineMapping> machineList = machineMappingRepository.findByDaiId(machineId);
        return createEkCycleReports(machineList, fromDate, toDate, exception);
    }
    private File getEkCycleReportsForLocation(String fromDate, String toDate, Integer exception, Integer locationId) {
        Location location = locationRepository.findOne(locationId);
        Iterable<Machine> machineList = location.getMachines();
        List<MachineMapping> mappingList = new ArrayList<MachineMapping>();
        for (Machine machine : machineList) {
            mappingList.add(machineMappingRepository.findOne(machine.getId()));
        }
        return createEkCycleReports(mappingList, fromDate, toDate, exception);
    }
    private File getEkCycleReportsForCompany(String fromDate, String toDate, Integer exception, Integer companyId) {
        Iterable<Location> locations = (companyRepository.findOne(companyId)).getLocations();
        List<MachineMapping> mappingList = new ArrayList<MachineMapping>();
        for (Location location : locations) {
            Iterable<Machine> machineList = location.getMachines();
            for (Machine machine : machineList) {
                mappingList.add(machineMappingRepository.findOne(machine.getId()));
            }
        }
        return createEkCycleReports(mappingList, fromDate, toDate, exception);
    }

    private List<Cycle> processException(String startDate, String endDate, Machine machine, Integer exceptionType) {
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

        DataSource source = null;
        EntityManagerFactory factory = null;
        try {
            source = appConfig.dataSource();
        } catch (Exception e) {
            logger.warn("failed to get data source", e.getMessage());
        }
        if (source != null) {
            factory = appConfig.entityManagerFactory(source, appConfig.jpaVendorAdapter()).getObject();
        }
        Query query = null;
        if (factory != null) {
            EntityManager em = factory.createEntityManager();
            query = em.createNamedQuery("Cycle.findWithNoException");
        }

        if (query != null) {
            switch (exceptionType) {
                case 0: {
                    Collection<DaiMeterActual> actuals = meterActualRepository.findByMachineIdAndReadingTimestampBetweenAndExceptionLikeOrNull(id, start, end, "");
                    if (!actuals.isEmpty()) {
                        result = cycleRepository.findByDaiMeterActualIn(actuals);
                    }
                    //no exceptions
//                    query.setParameter("id", id);
//                    query.setParameter("start", start);
//                    query.setParameter("end", end);
//                    query.setParameter("exception", "");
//                    result = (List<Cycle>) query.getResultList();
                    break;
                }
                case 1: {
                    result = cycleRepository.findByDateMachineAndRegex(id, start, end, "[1]");
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
                case 7: {
                    result = cycleRepository.findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionLike(id, start, end, "%7%");
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
                case 10: {
                    //all cold water
                    result = cycleRepository.findByDateMachineAndRegex(id, start, end, "[12]");
                    break;
                }
                case 20: {
                    //all hot water
                    result = cycleRepository.findByDateMachineAndRegex(id, start, end, "[34]");
                    break;
                }
                case 30: {
                    //all runtime
                    result = cycleRepository.findByDateMachineAndRegex(id, start, end, "[56]");
                    break;
                }
                case 40: {
                    //all low water
                    result = cycleRepository.findByDateMachineAndRegex(id, start, end, "[24]");
                    break;
                }
                case 50: {
                    //all high water
                    result = cycleRepository.findByDateMachineAndRegex(id, start, end, "[13]");
                    break;
                }
                case 60: {
                    //all water
                    result = cycleRepository.findByDateMachineAndRegex(id, start, end, "[1234]");
                    break;
                }
                case 100: {
                    //null exceptions
                    result = cycleRepository.findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionIsNull(id, start, end);
                }
            }
        }
        return result;
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

    private void initializeLogSheet(HSSFSheet sheet) {
        HSSFRow header = sheet.createRow(0);
//        CellStyle style = sheet.getWorkbook().createCellStyle();
//        HSSFFont font = sheet.getWorkbook().createFont();
//        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
//
//        style.setBorderBottom(CellStyle.BORDER_MEDIUM);
//        style.setFont(font);

        header.createCell(0).setCellValue("Company Name");
        header.createCell(1).setCellValue("Location Name");
        header.createCell(2).setCellValue("Machine Name");
        header.createCell(3).setCellValue("Last Cycle");

        for (int i = 0; i < header.getLastCellNum(); i++) {
            header.getCell(i).setCellStyle(getHeaderStyle(sheet.getWorkbook()));
        }
    }
    private void initializeSheet(Sheet sheet) {
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

        for (Cell cell: rowHeader) {
            cell.setCellStyle(getHeaderStyle((sheet.getWorkbook())));
        }
        for (int j=0; j < sheet.getRow(sheet.getLastRowNum()).getLastCellNum(); j++) {
            sheet.autoSizeColumn(j);
        }
    }
    private void initializeStatusGapSheet(HSSFSheet sheet) {
        HSSFRow header = sheet.createRow(0);
//        CellStyle style = sheet.getWorkbook().createCellStyle();
//        HSSFFont font = sheet.getWorkbook().createFont();
//        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
//        style.setBorderBottom(CellStyle.BORDER_MEDIUM);
//        style.setFont(font);

        header.createCell(0).setCellValue("Company Name");
        header.createCell(1).setCellValue("Location Name");
        header.createCell(2).setCellValue("Machine Name");
        header.createCell(3).setCellValue("Disconnected");
        header.createCell(4).setCellValue("Reconnected");
        header.createCell(5).setCellValue("Gap Length");

        for (int i=0; i<header.getLastCellNum(); i++) {
            header.getCell(i).setCellStyle(getHeaderStyle(sheet.getWorkbook()));
        }
    }
    private void initializeCompareSheet(HSSFSheet sheet) {
        HSSFRow header = sheet.createRow(0);

        header.createCell(0).setCellValue("Date");
        header.createCell(1).setCellValue("Formula");
        header.createCell(2).setCellValue("Start Time");
        header.createCell(3).setCellValue("Total Water");
        header.createCell(4).setCellValue("Expected Water");
        header.createCell(5).setCellValue("Variance");
        header.createCell(6).setCellValue("Percentage Variance");
        header.createCell(7).setCellValue("Daily Variance");
        header.createCell(8).setCellValue("Yearly Variance");
        header.createCell(9).setCellValue("Monthly Variance");
        header.createCell(10).setCellValue("Monthly Savings Not Captured");

        for (Cell cell : header) {
            cell.setCellStyle(getHeaderStyle(sheet.getWorkbook()));
        }
    }
    private void initializeSummarySheet(HSSFSheet sheet) {
        HSSFRow header = sheet.createRow(0);

        header.createCell(0).setCellValue("Formula");
        header.createCell(1).setCellValue("Average Variance Per Day");
        header.createCell(2).setCellValue("Average Variance Per Year");
        header.createCell(3).setCellValue("Average Variance Per Month");
        header.createCell(4).setCellValue("Average Monthly Savings Not Tracked");

        for (int i=0; i<header.getLastCellNum(); i++) {
            header.getCell(i).setCellStyle(getHeaderStyle(sheet.getWorkbook()));
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
        return ((ChronoLocalDateTime)status.getTimestamp().toLocalDateTime()).toEpochSecond(ZoneOffset.UTC);
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
    public File getCycleReports(String date) {
        return getCycleReports(date, date, 0);
    }

}

