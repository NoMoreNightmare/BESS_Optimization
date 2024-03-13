package Battery;

import java.math.BigDecimal;

/**
 * the battery object
 */
public abstract class Battery {
    /**
     * @return the maximum input and output power
     */
    public abstract BigDecimal getMaxIOPower();

    /**
     * @return the battery capacity
     */
    public abstract BigDecimal getMaxBatteryEnergy();

    /**
     * @return the remaining energy in the battery
     */
    public abstract BigDecimal getCurrentEnergy();

    /**
     * change the amount of energy remaining in the battery
     * @param energy
     */
    public abstract void changeCurrentEnergy(BigDecimal energy);

    /**
     * set the amount of energy in the battery
     * @param energy the energy in battery
     */
    public abstract void setCurrentEnergy(BigDecimal energy);

}
