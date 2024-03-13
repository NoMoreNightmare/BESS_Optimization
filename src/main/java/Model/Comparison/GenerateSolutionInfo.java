package Model.Comparison;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;

/**
 * determine the global optima of the result for each dataset
 * store the global optimal as additional information
 */
public class GenerateSolutionInfo {
    public static void main(String[] args) throws IOException {
        int[] length = {125,500,1000,3000,5000,10000};

        for(int len:length){
            generate(len);
        }
    }

    private static void generate(int len) throws IOException {
        File excelFile = new File(len+".xlsx");
        byte[] bytes = FileUtils.readFileToByteArray(excelFile);
        XSSFWorkbook sheets = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        Sheet sheet = sheets.getSheetAt(2);

        CellUtil.getCell(sheet.getRow(0),8).setCellValue("Global Optima");

        for(int i=1;i<=sheet.getLastRowNum();i++){
            double dp=sheet.getRow(i).getCell(0).getNumericCellValue();
            double milp=sheet.getRow(i).getCell(6).getNumericCellValue();

            double global=Math.min(dp,milp);
            CellUtil.getCell(sheet.getRow(i),8).setCellValue(global);
        }

        FileOutputStream fileOutputStream = new FileOutputStream(excelFile);
        sheets.write(fileOutputStream);

        sheets.close();
        fileOutputStream.close();
    }
}
