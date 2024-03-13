package Battery;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * the battery implement the degradation
 */
public class DegradationBattery extends NormalBattery{

    /**
     * total cost of the battery
     */
    BigDecimal batteryCost=new BigDecimal("10").setScale(8, RoundingMode.HALF_UP);
    /**
     * degradation rate
     */
    BigDecimal coulombicDegraRate =new BigDecimal("0.0001").setScale(8, RoundingMode.HALF_UP);
    /**
     * start life cycle
     */
    int cycle=1;
    /**
     * maximum life cycle
     */
    int maxCycle=1000;



    public DegradationBattery(BigDecimal maxBatteryEnergy) {
        super(maxBatteryEnergy);
    }

    /**
     * do degradation
     * @param demand current energy need to input or output
     * @return the partial cost
     */
    public BigDecimal degradeBattery(BigDecimal demand){
        if(demand.equals(new BigDecimal("0").setScale(8, RoundingMode.HALF_UP))){
            return new BigDecimal("0").setScale(8, RoundingMode.HALF_UP);
        }
        // the ratio of current battery capacity to origin battery capacity
        BigDecimal currentCapacityRatio= coulombicDegraRate.negate().multiply(new BigDecimal(cycle).setScale(8, RoundingMode.HALF_UP)).add(new BigDecimal("1"));
        BigDecimal maxBefore=new BigDecimal(maxBatteryEnergy.toString()).setScale(8, RoundingMode.HALF_UP);
        maxBatteryEnergy=originBatteryCapacity.multiply(currentCapacityRatio);

        //the life cycle is reduced according to the depth of discharge
        maxCycle=CalcuDepthOfDischarge(new BigDecimal("1").setScale(8, RoundingMode.HALF_UP).subtract(maxBatteryEnergy.divide(originBatteryCapacity))).intValue();

        //everytime call this method, the life cycle increase by 1
        cycle++;

        BigDecimal cost;

        //if the battery reach its life cycle, then replace with a new one
        if(cycle>=maxCycle){
            //(1/calcu(1)-1/calcu(1-maxbefore)/origin)*batteryCost
            cost = (new BigDecimal("1").setScale(8, RoundingMode.HALF_UP).divide(CalcuDepthOfDischarge(new BigDecimal("1"))).subtract(
                    new BigDecimal("1").setScale(8, RoundingMode.HALF_UP).divide(
                            CalcuDepthOfDischarge(new BigDecimal("1").setScale(8, RoundingMode.HALF_UP).subtract(maxBefore)).divide(originBatteryCapacity)
                    )
            )).multiply(batteryCost);
            reset();
            return cost;
        }

        //below calculate the total cost of the battery at each operation
        BigDecimal before = new BigDecimal("1").setScale(8, RoundingMode.HALF_UP).
                divide(CalcuDepthOfDischarge(new BigDecimal("1").setScale(8, RoundingMode.HALF_UP).
                                                subtract(maxBatteryEnergy.divide(originBatteryCapacity))));
        BigDecimal after = new BigDecimal("1").setScale(8, RoundingMode.HALF_UP).divide(CalcuDepthOfDischarge(new BigDecimal("1").
                subtract(maxBefore.divide(originBatteryCapacity))));

        cost=before.subtract(after);

        //the depth of discharge

        return cost;
    }

    private BigDecimal CalcuDepthOfDischarge(BigDecimal depthOfCharge) {
        //this is the life cycles in Li battery
        return new BigDecimal(694*Math.pow(depthOfCharge.doubleValue(),-0.795));
    }

    public void reset(){
        this.maxBatteryEnergy=new BigDecimal(originBatteryCapacity.toString());
        cycle=1;
    }


}
