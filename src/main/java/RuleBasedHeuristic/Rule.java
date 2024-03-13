package RuleBasedHeuristic;

import Battery.Battery;
import Battery.NormalBattery;
import Interface.Controllable;
import Model.LoadData;
import Model.Status;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * the abstract class of rule-based heuristic
 */
public abstract class Rule implements Controllable {
    BigDecimal[] loads;
    int timeInterval=24;
    int LOAD_LEVELS =2;
    BigDecimal PEAK_LIMIT =new BigDecimal("0.08").setScale(8, RoundingMode.HALF_UP);
    int TARIFF_LENGTH=24;
    BigDecimal[][] prices=new BigDecimal[timeInterval][LOAD_LEVELS];

    int loop;
    BigDecimal currentCost=new BigDecimal("0").setScale(8, RoundingMode.HALF_UP);

    Battery battery;

    String filename;
    public Rule(BigDecimal maxBatteryEnergy,String filename){
        battery =new NormalBattery(maxBatteryEnergy);
        this.filename = filename;
        for(int i=0;i<TARIFF_LENGTH;i++){
            if(i==2||i==3){
                prices[i][NORMAL]=new BigDecimal("1").setScale(8, RoundingMode.HALF_UP);
            }else if(i==10){
                prices[i][NORMAL]=new BigDecimal("4").setScale(8, RoundingMode.HALF_UP);
            }else{
                prices[i][NORMAL]=new BigDecimal("2").setScale(8, RoundingMode.HALF_UP);
            }
            prices[i][PEAK]=new BigDecimal("5").setScale(8, RoundingMode.HALF_UP);
        }

        try {
            loadData();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }



    }

    public Rule() {

    }

    /**
     * check the status of current load
     * @param timeInterval current time
     * @return the status
     */
    public Status checkStatus(int timeInterval){
        //exceed peak limit
        if(isPeak(timeInterval)){
            return Status.PEAK;
        // price is at a high level
        }else if(overHighThreshold(timeInterval)){
            return Status.HIGH_PRICE;
        //the price is cheap
        }else if(underLowThreshold(timeInterval)){
            return Status.LOW_PRICE;
        }
        return Status.NORMAL_PRICE;
    }

    boolean isPeak(int timeInterval){
        if(loads[timeInterval].doubleValue()> PEAK_LIMIT.doubleValue()){
            return true;
        }else{
            return false;
        }
    }

    //in current problem structure, the electricity price is fixed and known
    boolean overHighThreshold(int timeInterval){
        if(timeInterval%TARIFF_LENGTH==10){
            return true;
        }else{
            return false;
        }
    }

    boolean underLowThreshold(int timeInterval){
        if(timeInterval%TARIFF_LENGTH==2||timeInterval%TARIFF_LENGTH==3){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void loadData() throws IOException, CsvException {
        LoadData loadData=new LoadData(filename);
        try {
            loads=listToArray(loadData.loadTestData());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

        timeInterval=loads.length;

    }

    public BigDecimal[] listToArray(List<BigDecimal> subproblem) {
        BigDecimal[] loads=new BigDecimal[subproblem.size()];
        int i=0;
        for(BigDecimal f:subproblem){
            loads[i]=f;
            i++;
        }
        return loads;
    }

    @Override
    public void setLoop(int loop){
        this.loop=loop;
    }

    @Override
    public BigDecimal findMin(){
        return currentCost;
    }

    @Override
    public void printPath(){

    }

}

