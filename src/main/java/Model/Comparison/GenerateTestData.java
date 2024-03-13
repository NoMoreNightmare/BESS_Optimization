package Model.Comparison;

import Model.LoadData;
import com.opencsv.CSVWriter;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * generate different size of dataset from original dataset
 */
public class GenerateTestData {


    public static void main(String[] args) throws Exception {
        LoadData loadData = new LoadData("");
        List<BigDecimal> data;
        //the size of dataset
        int[] length = {125,500,1000,3000,5000,10000,15000};
        //generate 20 different dataset for each size
        for(int len:length) {
            for (int i = 0; i < 20; i++) {
                data = loadData.loadSomeDataFromOneRow(len);
                writeTestData(len,i,data);
                LoadData.index =-1;

                LoadData.startIndex = -1;
            }
//                

        }
    }

    /**
     * create new dataset
     * @param length the size of dataset
     * @param id the id of the dataset
     * @param data the data chosen to generate new data
     * @throws IOException
     */
    static void writeTestData(int length,int id,List<BigDecimal> data) throws IOException {
        String folder = "src/main/resources/"+length;
        String name = "/"+id+".csv";

        File folderObj = new File(folder);
        File file = new File(folder+name);
        if(!folderObj.exists()){
            folderObj.mkdir();
        }
        file.createNewFile();

        FileWriter outputfile = new FileWriter(file);
        CSVWriter csvWriter = new CSVWriter(outputfile);


        List<String> datasw = new ArrayList<String>();
        List<String> headers = new ArrayList<>();
        int head = 0;
        for(BigDecimal value:data){
            headers.add(String.valueOf(head));
            datasw.add(String.valueOf(value));
            head++;
        }
        String[] header = headers.toArray(new String[data.size()]);
        String[] writeData = datasw.toArray(new String[data.size()]);

        csvWriter.writeNext(header);
        csvWriter.writeNext(writeData);

        csvWriter.close();
//        System.gc();
    }
}
