package Model.Comparison;

import Interface.Controllable;
import Model.Result;
//import RuleBasedHeuristic.RuleHeuristicWithArima;
import Rolling.ImprovedRollingDP;
import Rolling.SimpleRollingMILP;
import RuleBasedHeuristic.RuleH;
import RuleBasedHeuristic.RuleWithPrediction;
import DynamicProgramming.BasicDynamicProgramming;
import DynamicProgramming.PrunedDynamicProgramming;
import com.opencsv.exceptions.CsvException;
import ilog.concert.IloException;
//import org.apache.poi.hssf.usermodel.HSSFRow;
//import org.apache.poi.hssf.usermodel.HSSFSheet;
//import org.apache.poi.hssf.usermodel.HSSFWorkbook;
//import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * generate all the results from all the algorithms on all the size of dataset
 */
public class GenerateResult {
    /**
     * the number of repeating
     */
    static int runNums = 1;
    static boolean exists = false;

    public static void main(String[] args) throws IOException, InvalidFormatException, IloException, CsvException {

        int[] length = {125,500,1000,3000,5000,10000};

        //reader different size of data, and run 20 times to get objective results
        for(int len:length){
            for(int i=0;i<20;i++){
                test(len,i);
            }
        }

    }

    public static void test(int length,int fileOrder) throws IOException, IloException, InvalidFormatException {


        String[] titles = {"dp","pruneddp","rollingdp","rollingmilp","ruleheuristic","ruleheuristicwithprediction"};

        String readFilename = length+"/"+fileOrder+".csv";
        String writeFilename = length+"fixed"+".xlsx";

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



        Map<String,List<Result>> result = data(readFilename);


        List<Result> time = result.get("Time");
        List<Result> memory = result.get("Memory");
        List<Result> solution = result.get("Solution");

        int lastIndex = sheets.timeSheet.getLastRowNum();

        for(int i=lastIndex+1;i<runNums+lastIndex+1;i++){
            setValue(sheets.timeSheet,time,i);
            setValue(sheets.memorySheet,memory,i);
        }

        lastIndex = sheets.solutionSheet.getLastRowNum();
        setValue(sheets.solutionSheet,solution,lastIndex+1);


        assert workbook != null;
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

            GenerateResult.exists=true;

            return new Sheets(timeSheet,memorySheet,solutionSheet);
        }else{
            return new Sheets(workbook.getSheet(time),workbook.getSheet(memory),workbook.getSheet(solution));
        }

    }

    private static void setValue(XSSFSheet sheet, List<Result> results,int i) {
        XSSFRow rown = sheet.createRow(i);
        Result result = results.get(0);
//        Result result = results.get(i-1);
        rown.createCell(0).setCellValue(result.getDp());
        rown.createCell(1).setCellValue(result.getPruneddp());
        rown.createCell(2).setCellValue(result.getRollingdp());
        rown.createCell(3).setCellValue(result.getRollingmilp());
        rown.createCell(4).setCellValue(result.getRuleH());
        rown.createCell(5).setCellValue(result.getRuleHPredict());
    }

    private static void setTitle(XSSFSheet Sheet) {
        XSSFRow row = Sheet.createRow(0);
        row.createCell(0).setCellValue("dp");
        row.createCell(1).setCellValue("pruneddp");
        row.createCell(2).setCellValue("rollingdp");
        row.createCell(3).setCellValue("rollingmilp");
        row.createCell(4).setCellValue("rule");
        row.createCell(5).setCellValue("rulewithpredict");
        row.createCell(6).setCellValue("MILP");
        row.setHeightInPoints(30);
    }

    private static Map<String,List<Result>> data(String file) throws IloException {

        Map<String,List<Result>> result = new HashMap<>();


        Triple dpTriple = runModel(new BasicDynamicProgramming(new BigDecimal("1"),200,file),runNums);
        Triple prunedDpTriple = runModel(new PrunedDynamicProgramming(new BigDecimal("1"),200,file),runNums);
        Triple rollingDpTriple = runModel(new ImprovedRollingDP(new BigDecimal("1"),100,file),runNums);
        Triple rollingMILPTriple = runModel(new SimpleRollingMILP(new BigDecimal("1"),file),runNums);
        Triple ruleHTriple = runModel(new RuleH(new BigDecimal("1"),file),runNums);
        Triple ruleHPredictTriple = runModel(new RuleWithPrediction(new BigDecimal("1"),file),runNums);

        List<Result> times = new ArrayList<>();
        List<Result> memories = new ArrayList<>();

        List<Result> solution = new ArrayList<>();
        for(int i=0;i<runNums;i++){
            Result resultTime = new Result();
            resultTime.setDp(dpTriple.times[i]);
            resultTime.setPruneddp(prunedDpTriple.times[i]);
            resultTime.setRollingdp(rollingDpTriple.times[i]);
            resultTime.setRollingmilp(rollingMILPTriple.times[i]);
            resultTime.setRuleH(ruleHTriple.times[i]);
            resultTime.setRuleHPredict(ruleHPredictTriple.times[i]);

            Result resultMemory = new Result();
            resultMemory.setDp(dpTriple.memories[i]);
            resultMemory.setPruneddp(prunedDpTriple.memories[i]);
            resultMemory.setRollingdp(rollingDpTriple.memories[i]);
            resultMemory.setRollingmilp(rollingMILPTriple.memories[i]);
            resultMemory.setRuleH(ruleHTriple.memories[i]);
            resultMemory.setRuleHPredict(ruleHPredictTriple.memories[i]);

            times.add(resultTime);
            memories.add(resultMemory);

            if(i==0){
                Result resultSolution = new Result();
                resultSolution.setDp(dpTriple.solution.doubleValue());
                resultSolution.setPruneddp(prunedDpTriple.solution.doubleValue());
                resultSolution.setRollingdp(rollingDpTriple.solution.doubleValue());
                resultSolution.setRollingmilp(rollingMILPTriple.solution.doubleValue());
                resultSolution.setRuleH(ruleHTriple.solution.doubleValue());
                resultSolution.setRuleHPredict(ruleHPredictTriple.solution.doubleValue());
                solution.add(resultSolution);
            }
        }
        result.put("Time",times);
        result.put("Memory",memories);
        result.put("Solution",solution);
        return result;
    }

    /**
     * run the algorithm and store the result
     * @param model the algorithm
     * @param runNums the number of repeating
     * @return the data structure storing the result
     * @throws IloException exception
     */
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

    /**
     * data structure storing the result
     */
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
