package DynamicProgramming;

//import ilog.concert.IloException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.lang.Math.min;

/**
 * DP with pruning strategies
 */
public class PrunedDynamicProgramming extends BasicDynamicProgramming {
    enum PriceLevel{
        //the price level is high or the load exceeds the peak limit causing high cost
        High,
        //the price is normal
        Normal,
        //the price is cheap
        Low
    }

    /**
     * no longer used
     */
    @Deprecated
    enum LoadTendency{
        //the load trend is upward
        UP,
        //the load trend is downward
        DOWN,
        //the load's trend varies has no pattern or the trend is stable
        STEADY
    }

    public PrunedDynamicProgramming(BigDecimal maxBatteryEnergy, int numOfNodes, String filename){
        super(maxBatteryEnergy,numOfNodes,filename);
        numberOfReached=(deltaBattery.equals(new BigDecimal(0).setScale(8, RoundingMode.HALF_UP))?0: battery.getMaxIOPower().divide(deltaBattery)).intValue();
    }


    /**
     * algorithm entry
     */
    @Override
    public void run(){
        //iterate all the time interval to solve the subproblems
        for(int i=0;i<timeInterval;i++){
            //LoadTendency loadTendency=loadTD(i);
            PriceLevel isPriceLow=priceLow(i);
            //iterate all the states to solve the subproblem at each state: the minimum cost to reach this state
            for(int j = 0; j< numOfNodes; j++){
                //if the node is not available, skip
                if(!nodes[i][j].isAvailable) {
                    continue;
                }
                for(Status status:Status.values()){
                    if(status==Status.NOACTION){
                        price=calculatePrice(i,new BigDecimal("0").setScale(8, RoundingMode.HALF_UP));
                        nodes[i+1][j].cost=min(nodes[i+1][j].cost, nodes[i][j].cost.add(price));
                        nodes[i+1][j].isAvailable=true;
                    }else if(status==Status.CHARGE){
                        //prune the charge path if the price is high
                        if(isPriceLow==PriceLevel.High){
                            continue;
                        }
                        //iterate all the possible amount of charge operations
                        for(int k=1;k<=numberOfReached;k++){
                            //don't exceed the battery capacity
                            if((j+k)>(numOfNodes -1)){
                                continue;
                            }
//                            if(loadTendency!=LoadTendency.UP){
                                price = calculatePrice(i, deltaBattery.multiply(new BigDecimal(k).setScale(8, RoundingMode.HALF_UP)));
                                if (nodes[i][j].cost.add(price).doubleValue() < nodes[i + 1][j+k].cost.doubleValue()) {
//                                    price=calculatePrice(i,k*deltaBattery);
                                    nodes[i+1][j+k].cost=min(nodes[i+1][j+k].cost, nodes[i][j].cost.add(price));
                                    nodes[i+1][j+k].isAvailable=true;
                                }
                        }
                    }else{
                        //prune the charge path if the price is cheap
                        if(isPriceLow==PriceLevel.Low){
                            continue;
                        }
                        //iterate all the possible amount of discharge operations
                        for(int k=1;k<=numberOfReached;k++){
                            //the battery energy should not be less than 0
                            if((j-k)<0){
                                continue;
                            }
//                            if(loadTendency!=LoadTendency.DOWN){
                                price = calculatePrice(i, deltaBattery.negate().multiply(new BigDecimal(k).setScale(8, RoundingMode.HALF_UP)));
                                // the discharged energy is greater than the loads
                                if(price.equals(BigDecimal.valueOf(Double.MAX_VALUE))){
                                    continue;
                                }
                                if (nodes[i][j].cost.add(price).doubleValue() < nodes[i + 1][j-k].cost.doubleValue()) {
//                                    price=calculatePrice(i,k*(-deltaBattery));
                                    nodes[i+1][j-k].cost=min(nodes[i+1][j-k].cost,nodes[i][j].cost.add(price));
                                    nodes[i+1][j-k].isAvailable=true;
                                }
                        }
                    }
                }
            }
        }
    }

    private BigDecimal min(BigDecimal cost, BigDecimal add) {
        if(cost.doubleValue()<add.doubleValue()){
            return cost;
        }else{
            return add;
        }
    }

    /**
     * no longer used
     * @param i
     * @return
     */
    private LoadTendency loadTD(int i) {
        if(i+1==timeInterval)
            return LoadTendency.STEADY;
        if((loads[i+1].subtract(loads[i])).doubleValue()>0){
            return LoadTendency.UP;
        }
        else if((loads[i+1].subtract(loads[i])).doubleValue()<0){
            return LoadTendency.DOWN;
        }else{
            return LoadTendency.STEADY;
        }
    }

    /**
     * calculate pruning flag
     * @param i current time interval
     * @return the flag
     */
    private PriceLevel priceLow(int i) {
        if(i%TARIFF_LENGTH==2||i%TARIFF_LENGTH==3) {
            return PriceLevel.Low;
        } else if (i%TARIFF_LENGTH==10||loads[i].doubleValue()>= PEAK_LIMIT.doubleValue()) {
            return PriceLevel.High;
        } else {
            return PriceLevel.Normal;
        }
    }

    @Override
    public void reset(){
        for(int i=0;i<nodes.length;i++){
            for(int j=0;j<nodes[i].length;j++){
                nodes[i][j]=new Node(deltaBattery.multiply(new BigDecimal(j).setScale(8, RoundingMode.HALF_UP)));
            }
        }
        nodes[0][0].cost=new BigDecimal("0").setScale(8, RoundingMode.HALF_UP);
        nodes[0][0].isAvailable=true;
        battery.setCurrentEnergy(battery.getMaxBatteryEnergy());
    }
}
