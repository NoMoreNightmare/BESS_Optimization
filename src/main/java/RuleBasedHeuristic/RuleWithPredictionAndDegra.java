package RuleBasedHeuristic;

import Battery.DegradationBattery;

import java.math.BigDecimal;

public class RuleWithPredictionAndDegra extends RuleWithPrediction{
    public RuleWithPredictionAndDegra(BigDecimal maxBatteryEnergy, String filename) {
        super(maxBatteryEnergy,filename);
        battery = new DegradationBattery(maxBatteryEnergy);
    }

    @Override
    public void run(){
        for(int i=0;i<timeInterval;i++){
            BigDecimal demand = getDemandedCDRate(i);

            //the degradation method help tp degradate the battery
            BigDecimal batteryCost = ((DegradationBattery)battery).degradeBattery(demand);

            currentCost = currentCost.add(calculatePrice(i,demand)).add(batteryCost);
        }
    }
}
