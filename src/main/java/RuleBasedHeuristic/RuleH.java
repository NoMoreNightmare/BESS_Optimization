package RuleBasedHeuristic;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RuleH extends Rule{

    public RuleH(BigDecimal maxBatteryEnergy, String filename) {
        super(maxBatteryEnergy,filename);
    }

    public RuleH(){
        super();

    }

    @Override
    public void run(){
        for(int i=0;i<timeInterval;i++){
            BigDecimal demand=getDemandedCDRate(i);

            currentCost=currentCost.add(calculatePrice(i,demand));
        }
    }


    public BigDecimal getDemandedCDRate(int timeInterval){
        BigDecimal maxIOPower= new BigDecimal(battery.getMaxIOPower().toString()).setScale(8, RoundingMode.HALF_UP);
        BigDecimal currentEnergy= new BigDecimal(battery.getCurrentEnergy().toString()).setScale(8, RoundingMode.HALF_UP);
        BigDecimal maxBatteryEnergy= new BigDecimal(battery.getMaxBatteryEnergy().toString()).setScale(8, RoundingMode.HALF_UP);
        //return negative value means discharge, on the contrary charge
        switch (checkStatus(timeInterval)){//this situation the max charge/discharge rate is
            case PEAK: {                      //impossible greater than the loads
                BigDecimal energy;
                if ((loads[timeInterval].subtract(PEAK_LIMIT)).doubleValue() > maxIOPower.doubleValue()) { //we need electricity more than battery can provide at that time
                    energy = min(currentEnergy, maxIOPower).negate();
                } else {                             //if we need electricity less than battery can provide at that time
                    energy = min(loads[timeInterval].subtract(PEAK_LIMIT), currentEnergy).negate();
                }
                battery.changeCurrentEnergy(energy);
                return new BigDecimal(energy.toString()).setScale(8, RoundingMode.HALF_UP);

            }
            case HIGH_PRICE: { //if the price is adjusted higher at that time and the load didn't exceed the peak limit
                BigDecimal energy;
                if (loads[timeInterval].doubleValue() > maxIOPower.doubleValue()) {     //if loads is greater than battery can provide at that time
                    energy = min(maxIOPower, currentEnergy);
                } else {
                    energy = min(loads[timeInterval], currentEnergy);
                }
                //            energy = -energy * (loads[timeInterval] / peakLimit);
                energy = energy.negate().multiply(currentEnergy.divide(maxBatteryEnergy));
                battery.changeCurrentEnergy(energy);
                return new BigDecimal(energy.toString()).setScale(8, RoundingMode.HALF_UP);
            }
            case LOW_PRICE: { //electricity price is cheap, charge
                if(currentEnergy.add(maxIOPower).doubleValue()>maxBatteryEnergy.doubleValue()){
                    if(loads[timeInterval].add(maxBatteryEnergy).subtract(currentEnergy).doubleValue()> PEAK_LIMIT.doubleValue()){
                        battery.changeCurrentEnergy(PEAK_LIMIT.subtract(loads[timeInterval]));
                        return PEAK_LIMIT.subtract(loads[timeInterval]);
                    }else{
                        battery.setCurrentEnergy(maxBatteryEnergy);
                        return maxBatteryEnergy.subtract(currentEnergy);
                    }
                }else{
                    if (maxBatteryEnergy.doubleValue() > (currentEnergy.add(maxIOPower)).doubleValue()) {
                        battery.changeCurrentEnergy(maxIOPower);
                        return new BigDecimal(maxIOPower.toString()).setScale(8, RoundingMode.HALF_UP);
                    } else {
                        BigDecimal energy = new BigDecimal(currentEnergy.toString()).setScale(8, RoundingMode.HALF_UP);
                        battery.setCurrentEnergy(maxBatteryEnergy);
                        return maxBatteryEnergy.subtract(energy);
                    }
                }
            }
            default: {
                BigDecimal actualCharge = maxIOPower.multiply(loads[timeInterval]).divide(PEAK_LIMIT);  //if the loads is getting closer to the peak limit, charge less
                if (currentEnergy.add(actualCharge).doubleValue() > maxBatteryEnergy.doubleValue()) {    //the amount of charge should not exceed SoC
                    BigDecimal energy = new BigDecimal(currentEnergy.toString()).setScale(8, RoundingMode.HALF_UP);
                    if ((loads[timeInterval].add(maxBatteryEnergy.subtract(energy)).doubleValue() > PEAK_LIMIT.doubleValue())) {   //the amount of charge plus loads should not exceed peak limit
                        battery.changeCurrentEnergy(PEAK_LIMIT.subtract(loads[timeInterval]));
                        return PEAK_LIMIT.subtract(loads[timeInterval]);
                    } else {
                        battery.setCurrentEnergy(maxBatteryEnergy);
                        return maxBatteryEnergy.subtract(energy);
                    }
                } else {
                    if ((loads[timeInterval].add(actualCharge)).doubleValue() > PEAK_LIMIT.doubleValue()) {
                        battery.changeCurrentEnergy(PEAK_LIMIT.subtract(loads[timeInterval]));
                        return PEAK_LIMIT.subtract(loads[timeInterval]);
                    } else {
                        battery.changeCurrentEnergy(actualCharge);
                        return new BigDecimal(actualCharge.toString()).setScale(8, RoundingMode.HALF_UP);
                    }
                }
            }
        }
    }

    public BigDecimal calculatePrice(int timeInterval, BigDecimal energyBattery) {
        BigDecimal totalE = loads[timeInterval].add(energyBattery);
        if (totalE.doubleValue() <= PEAK_LIMIT.doubleValue() && totalE.doubleValue() >= 0) {
            return totalE.multiply(prices[timeInterval % 24][NORMAL]);
        } else if (totalE.doubleValue() > PEAK_LIMIT.doubleValue()) {
            return PEAK_LIMIT.multiply(prices[timeInterval % 24][NORMAL]).add(
                    (totalE.subtract(PEAK_LIMIT)).multiply(prices[timeInterval % 24][PEAK]));
        } else {
            throw new ArithmeticException("energy went wrong");
        }
    }

    @Override
    public void reset(){
        currentCost=new BigDecimal("0").setScale(8, RoundingMode.HALF_UP);
        battery.setCurrentEnergy(battery.getMaxBatteryEnergy());
        System.gc();
    }

    private BigDecimal min(BigDecimal expr1, BigDecimal expr2){

        if(expr1.doubleValue()<expr2.doubleValue()){
            return expr1;
        }else{
            return expr2;
        }
    }
}
