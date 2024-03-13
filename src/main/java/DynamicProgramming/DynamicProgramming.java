package DynamicProgramming;

import Battery.Battery;
import Battery.NormalBattery;
import Interface.Controllable;
import Model.LoadData;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;


/**
 * abstract class of DP
 */
public abstract class DynamicProgramming implements Controllable {
    protected int timeInterval;
    protected final int TARIFF_LENGTH=24;
    protected BigDecimal[] loads;
    protected final int LOAD_LEVELS =2;
    protected BigDecimal[][] prices=new BigDecimal[TARIFF_LENGTH][LOAD_LEVELS];
    protected Battery battery;

    protected int numOfNodes;

    protected BigDecimal PEAK_LIMIT =new BigDecimal("0.08").setScale(8, RoundingMode.HALF_UP);

    protected Node[][] nodes;

    protected BigDecimal deltaBattery;

    protected int numberOfReached;
    int loop;
    String filename;

    public enum Status{
        //only charge operation is allowed
        CHARGE,
        //only discharge operation is allowed
        DISCHARGE,
        //no charge or discharge operation
        NOACTION;
    }

    /**
     * find the minimum cost of the problem
     *      the minimum cost stores in the states at the last time interval
     * @return
     */
    @Override
    public BigDecimal findMin() {
        BigDecimal min=BigDecimal.valueOf(Double.MAX_VALUE).setScale(8, RoundingMode.HALF_UP);
        for(int j = 0; j< numOfNodes; j++){
            if(nodes[timeInterval][j].cost.doubleValue()<min.doubleValue()){
                min= nodes[timeInterval][j].cost;
            }
        }
        return min;
    }

    /**
     * turning a arraylist object to array
     * @param subproblem problem instance
     * @return array
     */
    public BigDecimal[] listToArray(List<BigDecimal> subproblem) {
        BigDecimal[] loads=new BigDecimal[subproblem.size()];
        int i=0;
        for(BigDecimal f:subproblem){
            loads[i]=new BigDecimal(f.toString()).setScale(8, RoundingMode.HALF_UP);
            i++;
        }
        return loads;
    }

    /**
     * the method to apply model to solve the problem
     */
    @Override
    public abstract void run();

    /**
     * calculate the cost at the current tim interval
     * @param timeInterval curren time
     * @param energyBattery the amount of charge or discharge energy
     * @return the cost only at current time
     */
    public abstract BigDecimal calculatePrice(int timeInterval,BigDecimal energyBattery);

    /**
     * a method to test whether the algorithm works properly
     */
    @Override
    public void printPath(){
        BigDecimal min=BigDecimal.valueOf(Double.MAX_VALUE).setScale(8, RoundingMode.HALF_UP);
        int minJ=0;
        for(int j = 0; j< numOfNodes; j++){
            if(nodes[timeInterval][j].cost.doubleValue()<min.doubleValue()){
                min=nodes[timeInterval][j].cost;
                minJ=j;
            }
        }
        Node node=nodes[timeInterval][minJ];
        List<BigDecimal> list=new ArrayList<>();
        while(node.before!=null){
            list.add(0,node.energy);
            node=node.before;
        }
        for(BigDecimal a:list){
            System.out.print(a.doubleValue()+" ");
        }
        System.out.println();
    }

    @Override
    public void setLoop(int loop){
        this.loop=loop;
    }


    public DynamicProgramming(BigDecimal maxBatteryEnergy, int numOfNodes, String filename){
        //battery without degradation
        this.battery =new NormalBattery(maxBatteryEnergy);
        this.numOfNodes = numOfNodes;
        //the states for all time intervals
        //timeInterval+1 means include initial configuration
        nodes = new Node[timeInterval+1][numOfNodes];
        //the difference between two nearby states
        deltaBattery=maxBatteryEnergy.divide(new BigDecimal(numOfNodes).setScale(8, RoundingMode.HALF_UP));
        this.filename = filename;

        try {
            this.loadData();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }

        for(int i=0;i<TARIFF_LENGTH;i++){
            if(i==2||i==3){
                prices[i][NORMAL]=new BigDecimal("1").setScale(8, RoundingMode.HALF_UP);
            }else if(i==10){
                prices[i][NORMAL]=new BigDecimal("4").setScale(8, RoundingMode.HALF_UP);
            }else{
                prices[i][NORMAL]=new BigDecimal("2").setScale(8, RoundingMode.HALF_UP);
            }
            prices[i][PEAK]=new BigDecimal("5").setScale(8, RoundingMode.HALF_UP);
        }
        //the total number of states can reach = the max input or output power
        //                                       divided by the minimum amount of input or output power to reach a higher or lower state
        numberOfReached=deltaBattery.equals(new BigDecimal(0).setScale(8, RoundingMode.HALF_UP))?0: battery.getMaxIOPower().
                                        divide(deltaBattery).intValue();
    }

    @Override
    public void loadData() throws IOException, CsvException {
        LoadData loadData=new LoadData(filename);
        try {
            loads=listToArray(loadData.loadTestData());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

        timeInterval=loads.length;
        nodes = new Node[timeInterval+1][numOfNodes];
        for(int i=0;i<nodes.length;i++){
            for(int j=0;j<nodes[i].length;j++){
                nodes[i][j]=new Node(deltaBattery.multiply(new BigDecimal(j).setScale(8, RoundingMode.HALF_UP)));
            }
        }
        //the initial state's battery energy is 0, set related state available
        nodes[0][0].cost=new BigDecimal("0").setScale(8, RoundingMode.HALF_UP);
        nodes[0][0].isAvailable=true;

    }

}
