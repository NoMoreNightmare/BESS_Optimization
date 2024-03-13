package Battery;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * the ideal battery with no degradation
 */
public class NormalBattery extends Battery{

    BigDecimal maxBatteryEnergy;
    BigDecimal maxIOPower=new BigDecimal("0.2").setScale(2, RoundingMode.HALF_UP);
    BigDecimal originBatteryCapacity;
    BigDecimal currentEnergy=new BigDecimal("0").setScale(2, RoundingMode.HALF_UP);
    @Override
    public BigDecimal getMaxBatteryEnergy() {
        return maxBatteryEnergy;
    }

    @Override
    public BigDecimal getCurrentEnergy() {
        return currentEnergy;
    }

    @Override
    public void changeCurrentEnergy(BigDecimal energy) {
        currentEnergy=currentEnergy.add(energy);
    }

    @Override
    public void setCurrentEnergy(BigDecimal energy) {
        this.currentEnergy=new BigDecimal(energy.toString()).setScale(8, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getMaxIOPower() {
        return maxIOPower;
    }

    public NormalBattery(BigDecimal maxBatteryEnergy){
        this.maxBatteryEnergy=new BigDecimal(maxBatteryEnergy.toString()).setScale(8, RoundingMode.HALF_UP);
        this.originBatteryCapacity=new BigDecimal(maxBatteryEnergy.toString()).setScale(8, RoundingMode.HALF_UP);
    }


}
