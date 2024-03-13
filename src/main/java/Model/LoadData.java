package Model;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * load the data from csv files
 */
public class LoadData {

    public static int index = -1;
    public static int startIndex = -1;
    Random random = new Random();
    String filename;

    public LoadData(String filename){
        startIndex = -1;
        this.filename = filename;
    }

    public List<String[]> readCSVFile() throws IOException, CsvException {
        FileReader fileReader = new FileReader(getClass().getClassLoader().getResource("consumption.csv").getPath());
        CSVReader csvReader = new CSVReaderBuilder(fileReader).build();
        List<String[]> allData = csvReader.readAll();
        return allData;
    }

    /**
     * read the entire file
     * @return every rows in excel
     * @throws IOException
     * @throws CsvException
     */
    public List<List<BigDecimal>> loadData() throws IOException, CsvException {
        List<String[]> data=readCSVFile();
        List<List<BigDecimal>> consumptions=new ArrayList<>();

        data.remove(0);

        for(String[] s:data){
            List<BigDecimal> f=new ArrayList<>();
            for(String s1:s){
                if(s1==""||s1==null){
//                    continue;
                    f.add(new BigDecimal("0").setScale(3, RoundingMode.HALF_UP));
                }
                try{
                    f.add(new BigDecimal(s1).setScale(3, RoundingMode.HALF_UP));
                } catch(Exception e){
                    continue;
                }

            }
            consumptions.add(f);
        }

        return consumptions;
    }

    /**
     * this will return the data at that row (except for blank cell)
     * @param index start from 0, the data row
     * @return the loaded double data
     * @throws IOException
     * @throws CsvException
     */
    public List<BigDecimal> loadOneRow(int index) throws IOException, CsvException {
        String[] allData=readOneRow(index);
        List<BigDecimal> consumption=new ArrayList<>();


        for(String s1:allData){
            if(s1==""||s1==null){
                    continue;
            }
            try{
                consumption.add(new BigDecimal(s1).setScale(3, RoundingMode.HALF_UP));
            } catch(Exception e){
                continue;
            }

        }
        return consumption;
    }

    public String[] readOneRow(int index) throws FileNotFoundException {
        FileReader fileReader = new FileReader(getClass().getClassLoader().getResource("consumption.csv").getPath());
        CSVReader csvReader = new CSVReaderBuilder(fileReader).build();
        Iterator<String[]> iterable=csvReader.iterator();
        for(int i=0;i<index;i++){
            iterable.next();
        }
        return iterable.next();
    }

    public List<BigDecimal> loadSomeDataFromOneRow(int length) throws Exception {
        List<String[]> allDatas = readCSVFile();
        List<BigDecimal> consumption=new ArrayList<>();

        switch (length) {
            case 125:
                LoadData.index = 544 + random.nextInt(2000);

                break;
            case 500:
                LoadData.index = 544 + random.nextInt(2000);

                break;
            case 1000:
                LoadData.index = 1899 + random.nextInt(1000);

                break;
            case 3000:
                LoadData.index = 2170 + random.nextInt(1000);

                break;
            case 5000:
                LoadData.index = 2710 + random.nextInt(500);

                break;
            case 10000:
            case 15000:
                LoadData.index = 2980 + random.nextInt(200);

                break;
            default:
                LoadData.index = 3167;
                break;
        }

            String[] allData = allDatas.get(index + 1);
            for (String s1 : allData) {
                if (s1 == "" || s1 == null) {
                    continue;
                }
                try {
                    consumption.add(new BigDecimal(s1).setScale(3, RoundingMode.HALF_UP));
                } catch (Exception e) {
                    continue;
                }

            }
            do {
                if (consumption.size() < length) {
                    consumption.removeAll(consumption);
                    switch (length) {
                        case 125:
                            LoadData.index = 544 + random.nextInt(2000);

                            break;
                        case 500:
                            LoadData.index = 544 + random.nextInt(2000);

                            break;
                        case 1000:
                            LoadData.index = 1899 + random.nextInt(1000);

                            break;
                        case 3000:
                            LoadData.index = 2170 + random.nextInt(1000);

                            break;
                        case 5000:
                            LoadData.index = 2710 + random.nextInt(500);

                            break;
                        case 10000:
                        case 15000:
                            LoadData.index = 2980 + random.nextInt(200);

                            break;
                        default:
                            LoadData.index = 3167;
                            break;
                    }
                    allData = allDatas.get(index + 1);
                    for (String s1 : allData) {
                        if (s1 == "" || s1 == null) {
                            continue;
                        }
                        try {
                            consumption.add(new BigDecimal(s1));
                        } catch (Exception e) {
                            continue;
                        }

                    }
                } else {
                    if (startIndex == -1) {
                        startIndex = random.nextInt(consumption.size() - length);

//                startIndex = 0;
                        System.out.println(consumption.size() + " " + length);

                    }
                    break;
                }
            }while(consumption.size()<length);
        System.out.println(startIndex);



        return consumption.subList(startIndex,startIndex+length);
    }

    /**
     * load the data from dataset
     * @return
     * @throws IOException
     * @throws CsvException
     */
    public List<BigDecimal> loadTestData() throws IOException, CsvException {
        FileReader fileReader = new FileReader(getClass().getClassLoader().getResource(filename).getPath());
        CSVReader csvReader = new CSVReaderBuilder(fileReader).build();
        List<String[]> datas = csvReader.readAll();
        String[] data = datas.get(1);

        List<BigDecimal> consumption = new ArrayList<>();

        for (String s1 : data) {
            if (s1 == "" || s1 == null) {
                continue;
            }
            try {
                consumption.add(new BigDecimal(s1));
            } catch (Exception e) {
                continue;
            }

        }

        return consumption;
    }
}
