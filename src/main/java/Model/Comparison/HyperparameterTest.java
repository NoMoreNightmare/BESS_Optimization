package Model.Comparison;

import Interface.Controllable;
import Model.Result;
import Model.ResultForParameter;
import Rolling.ImprovedRollingDP;
import com.opencsv.exceptions.CsvException;
import ilog.concert.IloException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.*;

public class HyperparameterTest {
    private static boolean exists=false;
    private static Result result;

    public static void main(String[] args) throws IOException, InvalidFormatException, IloException, CsvException {
        int[] length = {3000};

        for(int len:length){
            for(int i=0;i<20;i++){
                test(len,i);
            }

        }

    }

    public static void test(int length,int fileOrder) throws IOException, IloException, InvalidFormatException {


        String readFilename = length+"/"+fileOrder+".csv";
        String writeFilename = length+"_parameter_tuning"+".xlsx";

        File excelFile = new File(writeFilename);
        if(!excelFile.exists()){
            try{
                exists = false;
                excelFile.createNewFile();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        OutputStream outputStream = new FileOutputStream(excelFile,exists);
        XSSFWorkbook workbook = null;
        if(!exists){
            workbook = new XSSFWorkbook();
        }
        else{
            workbook = new XSSFWorkbook(excelFile);

        }

        String timeName = "length"+length+"Time";
        String memoryName = "length"+length+"Memory";
        String solutionName = "length"+length+"Solution";

        Sheets sheets = initialize(workbook,timeName,memoryName,solutionName);



        Map<String, List<ResultForParameter>> result = data(readFilename);


        List<ResultForParameter> time = result.get("Time");
        List<ResultForParameter> memory = result.get("Memory");
        List<ResultForParameter> solution = result.get("Solution");

        int lastIndex = sheets.timeSheet.getLastRowNum();

        for(int i=lastIndex+1;i<1+lastIndex+1;i++){
            setValue(sheets.timeSheet,time,i);
            setValue(sheets.memorySheet,memory,i);
        }

        lastIndex = sheets.solutionSheet.getLastRowNum();
        setValue(sheets.solutionSheet,solution,lastIndex+1);


        workbook.setActiveSheet(0);
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();




    }
    private static Sheets initialize(XSSFWorkbook workbook, String time, String memory, String solution) {
        if(!exists){
            XSSFSheet timeSheet = workbook.createSheet(time);
            XSSFSheet memorySheet = workbook.createSheet(memory);
            XSSFSheet solutionSheet = workbook.createSheet(solution);

            setTitle(timeSheet);
            setTitle(memorySheet);
            setTitle(solutionSheet);

            exists=true;

            return new Sheets(timeSheet,memorySheet,solutionSheet);
        }else{
            return new Sheets(workbook.getSheet(time),workbook.getSheet(memory),workbook.getSheet(solution));
        }

    }

    private static void setValue(XSSFSheet sheet, List<ResultForParameter> results,int i) {
        XSSFRow rown = sheet.createRow(i);
        ResultForParameter result = results.get(0);
//        Result result = results.get(i-1);
        rown.createCell(0).setCellValue(result.getMu24());
        rown.createCell(1).setCellValue(result.getMu48());
        rown.createCell(2).setCellValue(result.getMu336());
        rown.createCell(3).setCellValue(result.getMu1440());
    }

    private static void setTitle(XSSFSheet Sheet) {
        XSSFRow row = Sheet.createRow(0);
        row.createCell(0).setCellValue("mu24");
        row.createCell(1).setCellValue("mu48");
        row.createCell(2).setCellValue("mu336");
        row.createCell(3).setCellValue("mu1440");
        row.setHeightInPoints(30);
    }

    /**
     * store the result of different hyperparameter values
     * @param file
     * @return
     * @throws IloException
     */
    private static Map<String,List<ResultForParameter>> data(String file) throws IloException {

        Map<String,List<ResultForParameter>> result = new HashMap<>();


        Triple mu24 = runModel(new ImprovedRollingDP(new BigDecimal("1"),100,file,24),1);
        Triple mu48 = runModel(new ImprovedRollingDP(new BigDecimal("1"),100,file,48),1);
        Triple mu336 = runModel(new ImprovedRollingDP(new BigDecimal("1"),100,file,336),1);
        Triple mu1440 = runModel(new ImprovedRollingDP(new BigDecimal("1"),100,file,1440),1);

        List<ResultForParameter> times = new ArrayList<>();
        List<ResultForParameter> memories = new ArrayList<>();

        List<ResultForParameter> solution = new ArrayList<>();
        for(int i=0;i<1;i++){
            ResultForParameter resultTime = new ResultForParameter();
            resultTime.setMu24(mu24.times[i]);
            resultTime.setMu48(mu48.times[i]);
            resultTime.setMu336(mu336.times[i]);
            resultTime.setMu1440(mu1440.times[i]);
            ResultForParameter resultMemory = new ResultForParameter();
            resultMemory.setMu24(mu24.memories[i]);
            resultMemory.setMu48(mu48.memories[i]);
            resultMemory.setMu336(mu336.memories[i]);
            resultMemory.setMu1440(mu1440.memories[i]);

            times.add(resultTime);
            memories.add(resultMemory);

            if(i==0){
                ResultForParameter resultSolution = new ResultForParameter();
                resultSolution.setMu24(mu24.solution.doubleValue());
                resultSolution.setMu48(mu48.solution.doubleValue());
                resultSolution.setMu336(mu336.solution.doubleValue());
                resultSolution.setMu1440(mu1440.solution.doubleValue());
                solution.add(resultSolution);
            }
        }
        result.put("Time",times);
        result.put("Memory",memories);
        result.put("Solution",solution);
        return result;
    }

    private static Triple runModel(Controllable model, int runNums) throws IloException {
        double[] resultTime = new double[runNums];
        double[] resultMemory = new double[runNums];
        BigDecimal solution = new BigDecimal("-1");
        System.gc();
        for(int i=0;i<runNums;i++){
            if(i>0){
                model.reset();
            }
            Runtime r = Runtime.getRuntime();
            r.gc();
            long beforeTime = System.currentTimeMillis();
            long beforeMemory = r.totalMemory()-r.freeMemory();
            model.run();
            long afterTime = System.currentTimeMillis();
            long afterMemory =r.totalMemory()- r.freeMemory();

            resultTime[i] = afterTime-beforeTime;
            resultMemory[i] = afterMemory-beforeMemory;
            if(i==0){
                solution = model.findMin();
            }
            System.gc();

        }

        return new Triple(solution,resultTime,resultMemory);
    }

    static class Triple{
        public BigDecimal solution;
        public double[] times;
        public double[] memories;

        public Triple(BigDecimal solution, double[] times, double[] memories) {
            this.solution = solution;
            this.times = times;
            this.memories = memories;
        }
    }

    static class Sheets{
        public Sheets(XSSFSheet timeSheet, XSSFSheet memorySheet, XSSFSheet solution) {
            this.timeSheet = timeSheet;
            this.memorySheet = memorySheet;
            this.solutionSheet = solution;
        }

        public XSSFSheet timeSheet;
        public XSSFSheet memorySheet;
        public XSSFSheet solutionSheet;


    }
}
