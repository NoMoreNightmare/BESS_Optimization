package Model.Comparison;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * with the result generated from the algorithms and the known global optima,
 * calculate the percentage of global optimal and success rate
 */
public class CalculateGlobalPercentAndSuccessRate {
    static double threshold = 0.005;
    public static void main(String[] args) throws IOException {
        int[] length = {125,500,1000,3000,5000,10000};

        for(int len:length){
            calculate(len);
        }
        calculateForHyperParameter();
    }

    /**
     * calculate the percentage and success rate for different size of dataset
     * @param len the size of dataset
     * @throws IOException the exception for file read and write
     */
    private static void calculate(int len) throws IOException {
        File excelFile = new File(len+".xlsx");
        byte[] bytes = FileUtils.readFileToByteArray(excelFile);
        XSSFWorkbook sheets = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        Sheet sheet = sheets.getSheetAt(2);

        CellUtil.getCell(sheet.getRow(0),8).setCellValue("Global Optima");
        //7 is the number of algorithms, 2 stands for the global optima percentage and success rate
        int NUMBER_OF_ALGORITHMS=7;
        int NUMBER_OF_CRITERIA=2;
        int globalColumn=8;
        int[][] nums = new int[NUMBER_OF_ALGORITHMS][NUMBER_OF_CRITERIA];
        int optimaRow=23;
        int successRow=24;
        sheet.createRow(optimaRow);
        sheet.createRow(successRow);
        CellUtil.getCell(sheet.getRow(optimaRow),NUMBER_OF_ALGORITHMS+1).setCellValue("global optima percentage");
        CellUtil.getCell(sheet.getRow(successRow),NUMBER_OF_ALGORITHMS+1).setCellValue("success rate");
        int ROW_NUMBER_OF_DATA=20;

        for(int column=0;column<NUMBER_OF_ALGORITHMS;column++){
            //column 23 for global optima and column 24 for success rate

            int optimaNum=0;
            int successNum=0;
            for(int row=1;row<=ROW_NUMBER_OF_DATA;row++){
                double solution = sheet.getRow(row).getCell(column).getNumericCellValue();
                double globalOptima = sheet.getRow(row).getCell(globalColumn).getNumericCellValue();
                if(Math.abs(solution-globalOptima)<=0.1){
                    optimaNum++;
                    successNum++;
                }else{
                    double lowBound=globalOptima*(1-threshold);
                    double highBound=globalOptima*(1+threshold);

                    if(solution>=lowBound&&solution<=highBound){
                        successNum++;
                    }
                }
            }
            CellUtil.getCell(sheet.getRow(optimaRow),column).setCellValue(optimaNum*1.0/ROW_NUMBER_OF_DATA);
            CellUtil.getCell(sheet.getRow(successRow),column).setCellValue(successNum*1.0/ROW_NUMBER_OF_DATA);

        }

        FileOutputStream fileOutputStream = new FileOutputStream(excelFile);
        sheets.write(fileOutputStream);

        sheets.close();
        fileOutputStream.close();
    }

    /**
     * calculate the percentage and success rate for hyperparameter tuning result
     * @throws IOException exception
     */
    private static void calculateForHyperParameter() throws IOException {
        File excelFile = new File("3000_parameter_tuning.xlsx");
        byte[] bytes = FileUtils.readFileToByteArray(excelFile);
        XSSFWorkbook sheets = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        Sheet sheet = sheets.getSheetAt(2);

        //7 is the number of algorithms, 2 stands for the global optima percentage and success rate
        int NUMBER_OF_PARAMETERS=4;
        int NUMBER_OF_CRITERIA=2;
        int globalColumn=5;
        int[][] nums = new int[NUMBER_OF_PARAMETERS][NUMBER_OF_CRITERIA];
        int optimaRow=23;
        int successRow=24;
        sheet.createRow(optimaRow);
        sheet.createRow(successRow);
        CellUtil.getCell(sheet.getRow(optimaRow),NUMBER_OF_PARAMETERS+1).setCellValue("global optima percentage");
        CellUtil.getCell(sheet.getRow(successRow),NUMBER_OF_PARAMETERS+1).setCellValue("success rate");
        int ROW_NUMBER_OF_DATA=20;

        for(int column=0;column<NUMBER_OF_PARAMETERS;column++){
            //column 23 for global optima and column 24 for success rate

            int optimaNum=0;
            int successNum=0;
            for(int row=1;row<=ROW_NUMBER_OF_DATA;row++){
                double solution = sheet.getRow(row).getCell(column).getNumericCellValue();
                double globalOptima = sheet.getRow(row).getCell(globalColumn).getNumericCellValue();
                if(Math.abs(solution-globalOptima)<=0.1){
                    optimaNum++;
                    successNum++;
                }else{
                    double lowBound=globalOptima*(1-threshold);
                    double highBound=globalOptima*(1+threshold);

                    if(solution>=lowBound&&solution<=highBound){
                        successNum++;
                    }
                }
            }
            CellUtil.getCell(sheet.getRow(optimaRow),column).setCellValue(optimaNum*1.0/ROW_NUMBER_OF_DATA);
            CellUtil.getCell(sheet.getRow(successRow),column).setCellValue(successNum*1.0/ROW_NUMBER_OF_DATA);

        }

        FileOutputStream fileOutputStream = new FileOutputStream(excelFile);
        sheets.write(fileOutputStream);

        sheets.close();
        fileOutputStream.close();
    }
}
