package DynamicProgramming;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Dynamic Programming for the problem
 */
public class BasicDynamicProgramming extends DynamicProgramming {

    /**
     * a reused variable in comparison
     */
    BigDecimal price;

    public BasicDynamicProgramming(BigDecimal maxBatteryEnergy, int Nbnodes, String filename) {
        super(maxBatteryEnergy, Nbnodes,filename);
    }

    /**
     * entry of the algorithm
     */
    @Override
    public void run() {
        //traverse the time intervals from the beginning
        for (int i = 0; i < timeInterval; i++) {
            //traverse all the states of the battery (different remaining energy)
            for (int j = 0; j < numOfNodes; j++) {
                //the state can not be reached, skip
                if (!nodes[i][j].isAvailable) {
                    continue;
                }
                //traverse all the possible actions a battery can do
                for (Status status : Status.values()) {
                    if (status == Status.NOACTION) {
                        //calculate current cost
                        price = calculatePrice(i, new BigDecimal(0).setScale(8, RoundingMode.HALF_UP));
                        //choose the minimum cost
                        if (nodes[i][j].cost.add(price).doubleValue() < nodes[i + 1][j].cost.doubleValue()) {
                            nodes[i + 1][j].cost = nodes[i][j].cost.add(price);
                            nodes[i + 1][j].before = nodes[i][j];
                            nodes[i + 1][j].isAvailable = true;
                        }
                    } else if (status == Status.CHARGE) {
                        //traverse the different amount of charge operations
                        for (int k = 1; k <= numberOfReached; k++) {
                            if ((j + k) > (numOfNodes -1)) {
                                continue;
                            }
                            price = calculatePrice(i, deltaBattery.multiply(new BigDecimal(k).setScale(8, RoundingMode.HALF_UP)));
                            if (nodes[i][j].cost.add(price).doubleValue() < nodes[i + 1][j + k].cost.doubleValue()) {
                                nodes[i + 1][j + k].cost = nodes[i][j].cost.add(price);
                                nodes[i + 1][j + k].before = nodes[i][j];
                                nodes[i + 1][j + k].isAvailable = true;

                            }
                        }


                    } else {
                        for (int k = 1; k <= numberOfReached; k++) {
                            if ((j - k) < 0) {
                                continue;
                            }
                            price = calculatePrice(i, deltaBattery.negate().multiply(new BigDecimal(k).setScale(8, RoundingMode.HALF_UP)));
                            if(price.equals(BigDecimal.valueOf(Double.MAX_VALUE))){
                                continue;
                            }
                            if (nodes[i][j].cost.add(price).doubleValue() < nodes[i + 1][j - k].cost.doubleValue()) {
                                nodes[i + 1][j - k].cost = nodes[i][j].cost.add(price);
                                nodes[i + 1][j - k].before = nodes[i][j];
                                nodes[i + 1][j - k].isAvailable = true;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public BigDecimal calculatePrice(int timeInterval, BigDecimal energyBattery) {
        BigDecimal totalE = loads[timeInterval].add(energyBattery);
        if (totalE.doubleValue() <= PEAK_LIMIT.doubleValue() && totalE.doubleValue() > 0) {
            return totalE.multiply(prices[timeInterval%24][NORMAL]);
           } else if (totalE.doubleValue() >= PEAK_LIMIT.doubleValue()) {
            BigDecimal expr1 = PEAK_LIMIT.multiply(prices[timeInterval%24][NORMAL]);
            BigDecimal expr2 = (totalE.subtract(PEAK_LIMIT)).multiply(prices[timeInterval % 24][PEAK]);
            return expr1.add(expr2);
        } else {
            return BigDecimal.valueOf(Double.MAX_VALUE);
        }

    }

    @Override
    public void reset() {
        for(int i=0;i<nodes.length;i++){
            for(int j=0;j<nodes[i].length;j++){
                nodes[i][j]=new Node(deltaBattery.multiply(new BigDecimal(j)));
            }
        }
        nodes[0][0].cost=new BigDecimal(0);
        nodes[0][0].isAvailable=true;
        battery.setCurrentEnergy(battery.getMaxBatteryEnergy());
    }
}
