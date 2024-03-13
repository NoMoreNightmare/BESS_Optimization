package Rolling;

import DynamicProgramming.Node;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * basic rolling horizon approach based on DP
 */
public class RollingDP extends Rolling{
    /**
     * the window size
     */
    int MU =48;
    /**
     * determine the degree to which bad solutions are acceptable
     */
    BigDecimal EPSILON=BigDecimal.valueOf(0.1);

    //initialize the problem
    public RollingDP(BigDecimal maxBatteryEnergy, int numOfNodes, String filename) {
        //the battery capacity
        super(maxBatteryEnergy,numOfNodes,filename);
    }

    public RollingDP(BigDecimal maxBatteryEnergy, int numOfNodes) {
        //the battery capacity
        super(maxBatteryEnergy,numOfNodes);
    }

    @Override
    public void run(){
        List<BigDecimal> l=this.problem;
        //the size of window which is equal to the problem size-the maximum window (size mu)*mu
        int beta=l.size()-roundNextMulti(l.size()-1, MU);

        int startIndex=0;
        int endIndex=beta;

        //the real time in the window
        int[] timeNow=new int[beta];
        for(int i=0;i<beta;i++){
            timeNow[i]=startIndex+i;
        }

        //create subproblem
        List<BigDecimal> subproblem=l.subList(startIndex,endIndex);
        BigDecimal[] loads=listToArray(subproblem);

        List<SoCCost> solutions=new ArrayList<>();

        //solve subproblems using DP and get solutions;
        this.resetLoads(solutions,loads,timeNow);
        dynamicProgramming();
        List<SoCCost> solution=this.findPath();
        solutions.addAll(solution);
        //traverse all the time intervals and fixed the operation one at each iteration
        //repeat the solving process for subproblem
        for(int j = 0; j<=(l.size()-1)/ MU -1; j++){
            startIndex= MU *j+beta;
            endIndex= MU *(j+1)+beta;
            subproblem=l.subList(startIndex,endIndex);
            loads=listToArray(subproblem);

            timeNow=new int[MU];
            for(int i = 0; i< MU; i++){
                timeNow[i]=startIndex+i;
            }

            this.resetLoads(solutions,loads,timeNow);
            dynamicProgramming();
            List<SoCCost> subsolution=this.findPath();
            solutions.addAll(subsolution);
        }

        this.solutions=solutions;
    }

    public void resetLoads(List<SoCCost> soccost, BigDecimal[] loads, int[] timeNow){
        this.loads=loads;
        this.timeInterval=loads.length;
        this.timeNow=timeNow;

        nodes = new Node[timeInterval+1][numOfNodes];
        deltaBattery= battery.getMaxBatteryEnergy().divide(new BigDecimal(numOfNodes).setScale(8, RoundingMode.HALF_UP));
        for(int i=0;i<nodes.length;i++){
            for(int j=0;j<nodes[i].length;j++){
                nodes[i][j]=new Node(deltaBattery.multiply(new BigDecimal(j).setScale(8, RoundingMode.HALF_UP)));
            }
        }

        if(soccost.size()==0){
            nodes[0][0].cost=new BigDecimal("0").setScale(8, RoundingMode.HALF_UP);
            nodes[0][0].isAvailable=true;
        }
        else{
            SoCCost soc=soccost.get(soccost.size()-1);

            for(int j = 0; j< numOfNodes; j++){
                if(nodes[0][j].energy.equals(soc.soc)){
                    nodes[0][j].cost=soc.cost;
                    nodes[0][j].isAvailable=true;
                }
            }
        }


    }

    @Override
    BigDecimal[] listToArray(List<BigDecimal> subproblem) {
        BigDecimal[] loads=new BigDecimal[subproblem.size()];
        int i=0;
        for(BigDecimal f:subproblem){
            loads[i]=new BigDecimal(f.toString()).setScale(8, RoundingMode.HALF_UP);
            i++;
        }
        return loads;
    }


    /**
     * use DP to solve the subproblem
     */
    public void dynamicProgramming() {

        for(int i=0;i<timeInterval;i++){
            for(int j = 0; j< numOfNodes; j++){
                if(!nodes[i][j].isAvailable) {
                    continue;
                }
                for(Status status: Status.values()){
                    if(status== Status.NoAction){
                        price=CalculatePrice(i,new BigDecimal("0").setScale(8, RoundingMode.HALF_UP));
                        if(nodes[i][j].cost.add(price).doubleValue()<nodes[i+1][j].cost.doubleValue()){
                            nodes[i+1][j].cost=nodes[i][j].cost.add(price);
                            nodes[i+1][j].before=nodes[i][j];
                            nodes[i+1][j].isAvailable=true;
                        }
                    }else if(status== Status.Charge){
                        for(int k=1;k<=numberOfReached;k++){
                            if((j+k)>(numOfNodes -1)){
                                continue;
                            }
                            price=CalculatePrice(i,deltaBattery.multiply(new BigDecimal(k).setScale(8, RoundingMode.HALF_UP)));
                            if(nodes[i][j].cost.add(price).doubleValue()<nodes[i+1][j+k].cost.doubleValue()){
                                nodes[i+1][j+k].cost=nodes[i][j].cost.add(price);
                                nodes[i+1][j+k].before=nodes[i][j];
                                nodes[i+1][j+k].isAvailable=true;
                            }
                        }

                    }else{
                        for(int k=1;k<=numberOfReached;k++){
                            if((j-k)<0){
                                continue;
                            }
                            price=CalculatePrice(i,deltaBattery.negate().multiply(new BigDecimal(k).setScale(8, RoundingMode.HALF_UP)));
                            if(price.equals(new BigDecimal(Double.MAX_VALUE))){
                                continue;
                            }
                            if(nodes[i][j].cost.add(price).doubleValue()<nodes[i+1][j-k].cost.doubleValue()){
                                nodes[i+1][j-k].cost=nodes[i][j].cost.add(price);
                                nodes[i+1][j-k].before=nodes[i][j];
                                nodes[i+1][j-k].isAvailable=true;
                            }
                        }
                    }
                }
            }
        }

    }


    /**
     * set the load in the subproblem
     * @param loads
     * @param soCCost
     */
    public void setLoads(BigDecimal[] loads, SoCCost soCCost){
        timeInterval=loads.length;
        this.loads=loads;
        nodes = new Node[timeInterval+1][numOfNodes];
        for(int i=0;i<nodes.length;i++){
            for(int j=0;j<nodes[i].length;j++){
                nodes[i][j]=new Node(deltaBattery.multiply(new BigDecimal(j)));
            }
        }

        if(soCCost==null){
            nodes[0][0].cost=new BigDecimal("0");
            nodes[0][0].isAvailable=true;
        }else{
            for(int j = 0; j< numOfNodes; j++){
                if(nodes[0][j].energy.equals(soCCost.soc)){
                    nodes[0][j].cost=soCCost.cost;
                    nodes[0][j].isAvailable=true;
                    break;
                }
            }
        }

    }

    @Override
    public void reset() {

    }
}
