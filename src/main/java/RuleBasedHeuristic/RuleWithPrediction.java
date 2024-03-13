package RuleBasedHeuristic;

import Model.Status;
import RuleBasedHeuristic.Prediction.LoadsPredictionModel;
import RuleBasedHeuristic.Prediction.Prediction;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RuleWithPrediction extends RuleH{
    LoadsPredictionModel predictionModel;
    public RuleWithPrediction(BigDecimal maxBatteryEnergy,String filename) {
        super(maxBatteryEnergy,filename);
        predictionModel = new LoadsPredictionModel(this.loads,this.PEAK_LIMIT);
    }

    @Override
    public BigDecimal getDemandedCDRate(int timeInterval) {
        Status status = checkStatus(timeInterval);
        Prediction operation = predictionModel.predict(timeInterval,status);

        BigDecimal maxIOPower= battery.getMaxIOPower();
        BigDecimal currentEnergy= battery.getCurrentEnergy();
        BigDecimal maxBatteryEnergy= battery.getMaxBatteryEnergy();

        switch (operation){
            //if no previous information, then run basic rule-based heuristic
            case NO_PREV_INFO: {
                return super.getDemandedCDRate(timeInterval);
            }
            //if the load exceeds the peak limit, discharge the excessive part as it can
            case PEAK_DISCHARGE: {
                BigDecimal energy;
                if ((loads[timeInterval].subtract(PEAK_LIMIT)).doubleValue() > maxIOPower.doubleValue()) { //we need electricity more than battery can provide at that time
                    energy = min(currentEnergy, maxIOPower).negate();
                } else {                             //if we need electricity less than battery can provide at that time
                    energy = min(loads[timeInterval].subtract(PEAK_LIMIT), currentEnergy).negate();
                }
                battery.changeCurrentEnergy(energy);
                return energy;
            }
            //if the load didn't exceed peak limit, and the price is normal
            case NON_PEAK_DISCHARGE:{
                BigDecimal energy;
                if(maxIOPower.doubleValue()>loads[timeInterval].doubleValue()){
                    energy = min(loads[timeInterval],currentEnergy).negate();
                } else{
                    energy = min(maxIOPower,currentEnergy).negate();
                }
                battery.changeCurrentEnergy(energy);
                return energy;
            }
            //don't do charge or discharge
            case NO_OPERATION:{
                return new BigDecimal("0").setScale(8, RoundingMode.HALF_UP);
            }
            //discharge limitedly
            case LIMITED_DISCHARGE:{
                //if the loads is getting closer to the peak limit, discharge less to deal with future peak limit
                BigDecimal actualCharge = maxIOPower.multiply(loads[timeInterval]).divide(PEAK_LIMIT);
                BigDecimal energy;
                if(actualCharge.doubleValue()>loads[timeInterval].doubleValue()){
                    energy = min(loads[timeInterval],currentEnergy).negate();
                } else{
                    energy = min(actualCharge,currentEnergy).negate();
                }
                battery.changeCurrentEnergy(energy);
                return energy;
            }
            case CHARGE:{
                return amountOfEnergy(timeInterval, maxIOPower, currentEnergy, maxBatteryEnergy);
            }
            case LIMITED_CHARGE:{
                //if the loads is getting closer to the peak limit, charge more to deal with future peak limit
                BigDecimal actualCharge = maxIOPower.multiply(loads[timeInterval]).divide(PEAK_LIMIT);
                return amountOfEnergy(timeInterval, actualCharge, currentEnergy, maxBatteryEnergy);
            }
            default:{
                System.out.println("something wrong");
                return new BigDecimal("-1").setScale(8, RoundingMode.HALF_UP);
            }
        }
    }

    private BigDecimal amountOfEnergy(int timeInterval, BigDecimal amountOfCharge, BigDecimal currentEnergy, BigDecimal maxBatteryEnergy) {
        if (currentEnergy.add(amountOfCharge).doubleValue() > maxBatteryEnergy.doubleValue()) {    //the amount of charge should not exceed SoC
            BigDecimal energy = currentEnergy;
            if ((loads[timeInterval].add(maxBatteryEnergy).subtract(energy)).doubleValue() > PEAK_LIMIT.doubleValue()) {   //the amount of charge plus loads should not exceed peak limit
                battery.changeCurrentEnergy(PEAK_LIMIT.subtract(loads[timeInterval]));
                return PEAK_LIMIT.subtract(loads[timeInterval]);
            } else {
                battery.setCurrentEnergy(maxBatteryEnergy);
                return maxBatteryEnergy.subtract(energy);
            }
        } else {
            if ((loads[timeInterval].add(amountOfCharge)).doubleValue() > PEAK_LIMIT.doubleValue()) {
                battery.changeCurrentEnergy(PEAK_LIMIT.subtract(loads[timeInterval]));
                return PEAK_LIMIT.subtract(loads[timeInterval]);
            } else {
                battery.changeCurrentEnergy(amountOfCharge);
                return amountOfCharge;
            }
        }
    }

    @Override
    public void reset(){
        currentCost=new BigDecimal("0").setScale(8, RoundingMode.HALF_UP);
        battery.setCurrentEnergy(battery.getMaxBatteryEnergy());
    }

    private BigDecimal min(BigDecimal expr1,BigDecimal expr2){
        if(expr1.doubleValue()<expr2.doubleValue()){
            return expr1;
        }else{
            return expr2;
        }
    }
}
