package Rolling;

import Battery.Battery;
import Battery.NormalBattery;
import Interface.Controllable;
import Model.LoadData;
import DynamicProgramming.Node;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * the abstract class of rolling horizon approach
 */
public abstract class Rolling implements Controllable {

    /**
     * the array stores the actual time in the window
     */
    public int[] timeNow;
    int timeInterval=24;
    Battery battery;

    BigDecimal[] loads;

    int LOAD_LEVELS =2;
    BigDecimal[][] prices=new BigDecimal[timeInterval][LOAD_LEVELS];

    int numOfNodes;

    BigDecimal PEAK_LIMIT =new BigDecimal("0.08").setScale(8, RoundingMode.HALF_UP);
    int numberOfReached;

    Node[][] nodes;

    BigDecimal deltaBattery;

    /**
     * @see DynamicProgramming.DynamicProgramming.Status
     */
    enum Status{
        Charge,
        DisCharge,
        NoAction;
    }
    BigDecimal price;

    final int TARIFF_LENGTH =24;


    /**
     *  solutions of the problem
     */
    List<SoCCost> solutions=new ArrayList<>();
    /**
     * problem instance
     */
    List<BigDecimal> problem;

    int loop;

    String filename;

    public Rolling(BigDecimal maxBatteryEnergy, int numOfNodes, String filename){
        this.battery =new NormalBattery(maxBatteryEnergy);
        this.filename = filename;

        //the number of states
        this.numOfNodes = numOfNodes;
        nodes = new Node[timeInterval+1][numOfNodes];
        deltaBattery=maxBatteryEnergy.divide(new BigDecimal(numOfNodes).setScale(8, RoundingMode.HALF_UP));

        //initialize the node with status, inside SoC and cost
        try {
            loadData();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }

        //price structure
        for(int i = 0; i< TARIFF_LENGTH; i++){
            if(i==2||i==3){
                prices[i][NORMAL]=new BigDecimal("1").setScale(8, RoundingMode.HALF_UP);
            }else if(i==10){
                prices[i][NORMAL]=new BigDecimal("4").setScale(8, RoundingMode.HALF_UP);
            }else{
                prices[i][NORMAL]=new BigDecimal("2").setScale(8, RoundingMode.HALF_UP);
            }
            prices[i][PEAK]=new BigDecimal("5").setScale(8, RoundingMode.HALF_UP);
        }
    }

    public Rolling(BigDecimal maxBatteryEnergy,int numOfNodes){
        this.battery =new NormalBattery(maxBatteryEnergy);

        //the number of possibilities of battery SoC we consider
        this.numOfNodes = numOfNodes;
        nodes = new Node[timeInterval+1][numOfNodes];
        deltaBattery=maxBatteryEnergy.divide(new BigDecimal(numOfNodes).setScale(8, RoundingMode.HALF_UP));


        //initialize the node with status, inside SoC and cost
        try {
            loadData();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }

        //price during one day
        for(int i = 0; i< TARIFF_LENGTH; i++){
            if(i==2||i==3){
                prices[i][NORMAL]=new BigDecimal("1").setScale(8, RoundingMode.HALF_UP);
            }else if(i==10){
                prices[i][NORMAL]=new BigDecimal("4").setScale(8, RoundingMode.HALF_UP);
            }else{
                prices[i][NORMAL]=new BigDecimal("2").setScale(8, RoundingMode.HALF_UP);
            }
            prices[i][PEAK]=new BigDecimal("5").setScale(8, RoundingMode.HALF_UP);
        }
    }


    public List<SoCCost> findPath(){
        BigDecimal min=BigDecimal.valueOf(Double.MAX_VALUE);
        int minJ=0;

        //find minimum cost after considering all the loads
        for(int j = 0; j< numOfNodes; j++){
            if(nodes[timeInterval][j].cost.doubleValue()<min.doubleValue()){
                min= nodes[timeInterval][j].cost;
                minJ=j;
            }
        }

        //get the state with minimum
        Node node=nodes[timeInterval][minJ];
        List<SoCCost> list=new ArrayList<>();

        //every state has a before node, which indicates where the state comes from
        //node.before will find the previous state which reach this state until there is no states
        while(node!=null){
            SoCCost soCCost=new SoCCost(node.energy,node.cost);

            //add at first to reverse the order(final->final-1->...->0, insert at head can recover the order)
            list.add(0,soCCost);
            node=node.before;
        }

        return list;

    }

    @Override
    public void printPath(){
        List<SoCCost> l=findPath();
        for(int i=0;i<l.size();i++){
            System.out.print(l.get(i).soc+" ");
        }
        System.out.println();
    }

    @Override
    public BigDecimal findMin() {
        BigDecimal min=BigDecimal.valueOf(Double.MAX_VALUE);
        for(int j = 0; j< numOfNodes; j++) {
            if (nodes[timeInterval][j].cost.doubleValue() < min.doubleValue()) {
                min = nodes[timeInterval][j].cost;
            }
        }
        return min;
    }

    BigDecimal[] listToArray(List<BigDecimal> subproblem) {
        BigDecimal[] loads=new BigDecimal[subproblem.size()];
        int i=0;
        for(BigDecimal f:subproblem){
            loads[i]=new BigDecimal(f.toString());
            i++;
        }
        return loads;
    }

    /**
     * return the maximum number of window (with size mu) can be created in the problem
     * @param problemSize
     * @param mu
     * @return
     */
    int roundNextMulti(int problemSize, int mu) {
        while(problemSize>=0){
            if(problemSize%mu==0){
                break;
            }
            problemSize=problemSize-1;
        }

        return problemSize;
    }

    BigDecimal CalculatePrice(int timeInterval,BigDecimal energyBattery) {
        BigDecimal totalE=loads[timeInterval].add(energyBattery);
        if(totalE.doubleValue()<= PEAK_LIMIT.doubleValue()&&totalE.doubleValue()>0){
            return totalE.multiply(prices[timeNow[timeInterval]%24][NORMAL]);
        }
        else if(totalE.doubleValue()> PEAK_LIMIT.doubleValue()){
            return PEAK_LIMIT.multiply(prices[timeNow[timeInterval]%24][NORMAL]).add((totalE.subtract(PEAK_LIMIT)).multiply(prices[timeNow[timeInterval]%24][PEAK]));
        }
        else{
            return new BigDecimal(Double.MAX_VALUE);
        }
    }

    /**
     * no longer used
     * @param filename
     * @throws IOException
     * @throws CsvException
     */
    @Deprecated
    public void loadData(String filename) throws IOException, CsvException{
        LoadData loadData=new LoadData(filename);
        try {
            problem=loadData.loadTestData();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
        loads=listToArray(problem);

        timeInterval=loads.length;
    }

    @Override
    public void loadData() throws IOException, CsvException{
        LoadData loadData=new LoadData(filename);
        try {
            problem=loadData.loadTestData();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
        loads=listToArray(problem);

        timeInterval=loads.length;
    }

    @Deprecated
    public List<SoCCost> getSolutions(){
        return solutions;
    }

    @Override
    public void setLoop(int loop){
        this.loop=loop;
    }
}
