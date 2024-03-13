package Rolling;

import DynamicProgramming.Node;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * the improved rolling horizon approach based on DP
 */
public class ImprovedRollingDP extends RollingDP{

    /**
     * the length of original problem
     */
    private int totalLength;

    public ImprovedRollingDP(BigDecimal maxBatteryEnergy, int numOfNodes, String filename) {
        super(maxBatteryEnergy, numOfNodes,filename);
        List<BigDecimal> l=problem;

        this.timeInterval=l.size();
        totalLength=timeInterval;
        deltaBattery=maxBatteryEnergy.divide(new BigDecimal(numOfNodes).setScale(8, RoundingMode.HALF_UP));
        this.numOfNodes =numOfNodes;

        numberOfReached=deltaBattery.equals(new BigDecimal(0).setScale(8, RoundingMode.HALF_UP))?0: battery.getMaxIOPower().divide(deltaBattery).intValue();
    }

    /**
     * for hyperparameter setting
     * @param maxBatteryEnergy battery capacity
     * @param numOfNodes number of states
     * @param filename data file
     * @param mu window size
     */
    public ImprovedRollingDP(BigDecimal maxBatteryEnergy, int numOfNodes, String filename,int mu) {
        super(maxBatteryEnergy, numOfNodes,filename);
        List<BigDecimal> l=problem;
        this.MU=mu;
        this.timeInterval=l.size();
        totalLength=timeInterval;
        deltaBattery=maxBatteryEnergy.divide(new BigDecimal(numOfNodes).setScale(8, RoundingMode.HALF_UP));
        this.numOfNodes =numOfNodes;

        numberOfReached=deltaBattery.equals(new BigDecimal(0).setScale(8, RoundingMode.HALF_UP))?0: battery.getMaxIOPower().divide(deltaBattery).intValue();
    }

    @Override
    public void run(){
        int beta=timeInterval-roundNextMulti(timeInterval-1, MU);
        int start=0;
        int end= MU;
        List<BigDecimal> subproblem=this.problem.subList(start,end);
        loads=listToArray(subproblem);
        List<SoCCost> solutions=new ArrayList<>();

        SoCCost previous=null;
        this.reset(previous);
        setTimeNow(start,end);

        //solve the subproblem from 0 to MU using DP
        dynamicProgramming();
        List<SoCCost> solution=this.findPath();
        solutions.addAll(solution);
        setTimeNow(0,beta);
        //get the optimal solution of subproblem from 0 to beta
        BigDecimal optima=findOptima(listToArray(problem.subList(0,beta)),previous,timeNow);

        //traverse the time intervals within first window (1~beta);
        // the solution at time interval 0 is fixed when first solve the subproblem
        // each time solve subproblem from t to t+beta, but focus on the solution stores at time interval beta and compare it with the optima
        for(int t=1;t<=beta;t++){
            List<SoCCost> temp=cloneSolution(solutions);
            previous=temp.get(t);
            setTimeNow(t,t+ MU);
            loads=listToArray(problem.subList(t,t+ MU));
            this.reset(previous);

            //solve new subproblem within the window
            dynamicProgramming();

            List<SoCCost> newSolution=findPath();

            solutions=setSubList(solutions,newSolution.subList(1,newSolution.size()),t,t+ MU);
            //if the solution is worsen than allowed, then drop the new solution of subproblem and restore previous solution
            if(solutions.get(beta-1).cost.doubleValue()>(EPSILON.add(new BigDecimal("1").setScale(8, RoundingMode.HALF_UP)).multiply(optima)).doubleValue()){
                solutions=temp;
            }
        }

        // same as above, but for the window size mu; solve the fixed subproblem,
        // then move the window and compare new solution to original
        for(int t = 0; t<=(totalLength-1)/ MU -2; t++){
            previous=solutions.get(t* MU +beta-1);
            this.reset(previous);
            setTimeNow(MU *t+beta, MU *(t+1)+beta);
            optima=findOptima(listToArray(problem.subList(MU *t+beta, MU *(t+1)+beta)),previous,timeNow);

            for(int j = beta+1; j<= MU +beta; j++){
//                System.out.println(j);
                List<SoCCost> temp=cloneSolution(solutions);
                previous=temp.get(MU *t+j-1);

                loads=listToArray(problem.subList(MU *t+j, MU *(t+1)+j));
                this.reset(previous);
                setTimeNow(MU *t+j, MU *(t+1)+j);

                dynamicProgramming();

                List<SoCCost> newSolution=findPath();

                solutions=setSubList(solutions,newSolution.subList(1, newSolution.size()), MU *t+j, MU *(t+1)+j);
                if(solutions.get(MU *(t+1)+beta-1).cost.doubleValue()>(EPSILON.add(new BigDecimal("1").setScale(8, RoundingMode.HALF_UP))).multiply(optima).doubleValue()){
                    solutions=temp;
                }
            }
        }

        this.solutions=solutions;


    }

    /**
     * store the current time for the window
     * @param start the start time of the window
     * @param end the end time of the window
     */
    protected void setTimeNow(int start, int end) {
        timeNow=new int[end-start];
        for(int i=0;i<end-start;i++){
            timeNow[i]=start+i;
        }
    }

    /**
     * deep clone a solution
     * @param solutions the solution
     * @return a same content solution
     */
    public List<SoCCost> cloneSolution(List<SoCCost> solutions) {
        List<SoCCost> clone=new ArrayList<>(solutions.size());
        for(SoCCost soCCost:solutions){
            clone.add(new SoCCost(soCCost.soc, soCCost.cost ));
        }
        return clone;
    }


    /**
     * find the optimal solution of the subproblems using DP
     * @param loads the subproblem's instance
     * @param previous the previous time interval's information
     * @param timeNow the time of the window
     * @return the optimal of the solution
     */
    protected BigDecimal findOptima(BigDecimal[] loads, SoCCost previous, int[] timeNow) {


        RollingDP dp=new RollingDP(battery.getMaxBatteryEnergy(), numOfNodes,filename);
        dp.setLoads(loads,previous);

        dp.timeNow=timeNow;
        dp.dynamicProgramming();

        return dp.findMin();
    }


    /**
     * store the solution of subproblem
     * @param list the list store the solution of the initial problem
     * @param sublist the solution of subproblem
     * @param start the start time of the solution
     * @param end the end time of the solution
     * @return the list storing the solution of the initial problem with new update
     */
    public List<SoCCost> setSubList(List<SoCCost> list,List<SoCCost> sublist,int start,int end){
        for(int i=start;i<end;i++){
            if(list.size()<=i){
                list.add(i,sublist.get(i-start));
            }
            else{
                list.set(i,sublist.get(i-start));
            }
        }
        return list;
    }

    /**
     * reset the loads, time and states
     * @param previousEnd the previous time interval's information
     */
    public void reset(SoCCost previousEnd){
        timeInterval=loads.length;
        nodes = new Node[timeInterval+1][numOfNodes];

        for(int i=0;i<nodes.length;i++){
            for(int j=0;j<nodes[i].length;j++){
                nodes[i][j]=new Node(deltaBattery.multiply(new BigDecimal(j)));
            }
        }

        if(previousEnd==null){
            nodes[0][0].cost=new BigDecimal("0");
            nodes[0][0].isAvailable=true;
        }
        else{
            for(int j = 0; j< numOfNodes; j++){
                if(nodes[0][j].energy.equals(previousEnd.soc)){
                    nodes[0][j].cost=previousEnd.cost;
                    nodes[0][j].isAvailable=true;
                }
            }
        }
    }

    @Override
    public void reset(){
        timeInterval = problem.size();
        nodes = new Node[timeInterval][LOAD_LEVELS];
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