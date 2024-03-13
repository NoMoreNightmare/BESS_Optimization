/*
package RuleBasedHeuristic;

import Battery.*;
import Interface.ControlModel;
import Model.ArimaLoadsPrediction;
import Model.LoadData;
import RuleBasedHeuristic.Prediction.Prediction;
import Model.Status;
import com.opencsv.exceptions.CsvException;
import com.workday.insights.timeseries.arima.Arima;
import com.workday.insights.timeseries.arima.struct.ArimaParams;
import com.workday.insights.timeseries.arima.struct.ForecastResult;



import java.io.IOException;
import java.util.ArrayList;

public class RuleHeuristicWithArima extends RuleH {

    ArrayList<Double> loadsList;

    int p = 10;
    int d = 0;
    int q = 6;
    int P = 4;
    int D = 4;
    int Q = 0;
    int m = 0;
    int forecastSize = 21;

    int WEEK = 90;

    public RuleHeuristicWithArima(double maxBatteryEnergy) {
        super();
        battery =new NormalBattery(maxBatteryEnergy);

        for(int i=0;i<24;i++){
            prices[i][0]=1;
            if(i==2||i==3){
                prices[i][1]=1;
            }else if(i==10){
                prices[i][1]=4;
            }else{
                prices[i][1]=2;
            }
            prices[i][2]=5;
        }

        try {
            loadData();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadData() throws IOException, CsvException {
        LoadData loadData=new LoadData();
//        List<List<Double>> consumptions=loadData.loadData();

        //take index 3167 as an example
        loadsList=(ArrayList<Double>) loadData.loadOneRow(3167);
        loads=listToArray(loadsList);

        timeInterval=loads.length;
    }


    @Override
    public double getDemandedCDRate(int timeInterval){
        if(timeInterval<WEEK){
            return super.getDemandedCDRate(timeInterval);
        }
        else{
            ForecastResult forecastResult = Arima.forecast_arima(listToArray(loadsList.subList(timeInterval-WEEK,timeInterval)),forecastSize,new ArimaParams(p,d,q,P,D,Q,m));

            double[] futureData = forecastResult.getForecast();

            Status status = checkStatus(timeInterval);
            ArimaLoadsPrediction arimaLoadsPrediction = new ArimaLoadsPrediction(loads,peakLimit);

            Prediction predict = arimaLoadsPrediction.predict(timeInterval, status, WEEK, futureData);


            double maxIOPower= battery.getMaxIOPower();
            double currentEnergy= battery.getCurrentEnergy();
            double maxBatteryEnergy= battery.getMaxBatteryEnergy();

            switch (predict) {
                case NO_PREV_INFO -> {
                    return super.getDemandedCDRate(timeInterval);
                }
                case PEAK_DISCHARGE -> {
                    double energy;
                    if ((loads[timeInterval] - peakLimit) > maxIOPower) { //we need electricity more than battery can provide at that time
                        energy = -Math.min(currentEnergy, maxIOPower);
                    } else {                             //if we need electricity less than battery can provide at that time
                        energy = -Math.min(loads[timeInterval] - peakLimit, currentEnergy);
                    }
                    battery.changeCurrentEnergy(energy);
                    return energy;
                }
                case NON_PEAK_DISCHARGE -> {
                    double energy;
                    if (maxIOPower > loads[timeInterval]) {
                        energy = -Math.min(loads[timeInterval], currentEnergy);
                    } else {
                        energy = -Math.min(maxIOPower, currentEnergy);
                    }
                    battery.changeCurrentEnergy(energy);
                    return energy;
                }
                case NO_OPERATION -> {
                    return 0;
                }
                case LIMITED_DISCHARGE -> {
                    double actualCharge = maxIOPower * (peakLimit - loads[timeInterval]) / peakLimit;
                    double energy;
                    if (actualCharge > loads[timeInterval]) {
                        energy = -Math.min(loads[timeInterval], currentEnergy);
                    } else {
                        energy = -Math.min(actualCharge, currentEnergy);
                    }
                    battery.changeCurrentEnergy(energy);
                    return energy;
                }
                case CHARGE -> {
                    return amountOfEnergy(timeInterval, maxIOPower, currentEnergy, maxBatteryEnergy);
                }
                case LIMITED_CHARGE -> {
                    double actualCharge = maxIOPower * (peakLimit - loads[timeInterval]) / peakLimit;  //if the loads is getting closer to the peak limit, charge less
                    return amountOfEnergy(timeInterval, actualCharge, currentEnergy, maxBatteryEnergy);
                }
                default -> {
                    return -1;
                }
            }
        }
    }

    public Status checkStatus(int timeInterval){
        if(isPeak(timeInterval)){
            return Status.PEAK;
        }else if(overHighThreshold(timeInterval)){
            return Status.HIGH_PRICE;
        }else if(underLowThreshold(timeInterval)){
            return Status.LOW_PRICE;
        }
        return Status.NORMAL_PRICE;
    }


    private double amountOfEnergy(int timeInterval, double amountOfCharge, double currentEnergy, double maxBatteryEnergy) {
        if (currentEnergy + amountOfCharge > maxBatteryEnergy) {    //the amount of charge should not exceed SoC
            double energy = currentEnergy;
            if ((loads[timeInterval] + maxBatteryEnergy - energy) > peakLimit) {   //the amount of charge plus loads should not exceed peak limit
                battery.changeCurrentEnergy(peakLimit - loads[timeInterval]);
                return peakLimit - loads[timeInterval];
            } else {
                battery.setCurrentEnergy(maxBatteryEnergy);
                return maxBatteryEnergy - energy;
            }
        } else {
            if ((loads[timeInterval] + amountOfCharge) > peakLimit) {
                battery.changeCurrentEnergy(peakLimit - loads[timeInterval]);
                return peakLimit - loads[timeInterval];
            } else {
                battery.changeCurrentEnergy(amountOfCharge);
                return amountOfCharge;
            }
        }
    }


}
*/