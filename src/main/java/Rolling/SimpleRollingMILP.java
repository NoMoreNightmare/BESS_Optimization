package Rolling;

import Battery.Battery;
import Battery.NormalBattery;
import Interface.Controllable;
import Model.LoadData;
import com.opencsv.exceptions.CsvException;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class SimpleRollingMILP implements Controllable {
    int timeInterval=24;
    Battery normalBattery;

    double[] loads;
    int MU =48;
    int TARIFF_LENGTH =24;

    int LOAD_LEVELS =2;
    double[][] prices=new double[TARIFF_LENGTH][LOAD_LEVELS];


    double PEAK_LIMIT =0.08;


    int loop;

    double startEnergy=0;

    double[][] realEnergy;

    String filename;

    public SimpleRollingMILP(BigDecimal maxBatteryEnergy, String filename)  {
        this.normalBattery=new NormalBattery(maxBatteryEnergy);
        this.filename = filename;

        //initialize the node with status, inside SoC and cost
        try {
            loadData();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
        //price during one day
        for(int i = 0; i< TARIFF_LENGTH; i++){
            if(i==2||i==3){
                prices[i][NORMAL]=1;
            }else if(i==10){
                prices[i][NORMAL]=4;
            }else{
                prices[i][NORMAL]=2;
            }
            prices[i][PEAK]=5;
        }
    }

    public double MILP(int length,int startTime,double startBatteryEnergy) throws IloException {
        //build the model

        IloCplex cplex=new IloCplex();
        cplex.setOut(OutputStream.nullOutputStream());

        //define parameters 只有decision variables需要，其他的直接用java数组的形式定义和参与计算
        IloNumVar[][] energy=new IloNumVar[length][];
        for(int i=0;i<length;i++){
            //the energy at each time interval and different part should be greater or equal to 0
            energy[i]=cplex.numVarArray(LOAD_LEVELS,0,Double.MAX_VALUE);
        }

        //the battery energy should be less than the battery capacity and greater than 0
        IloNumVar[] realTimeEnergy=cplex.numVarArray(length+1,0,normalBattery.getMaxBatteryEnergy().setScale(2,RoundingMode.HALF_UP).doubleValue());


        //set objective function
        IloNumExpr cost=cplex.numExpr();
        for(int i=0;i<length;i++){
            for(int j = 0; j< LOAD_LEVELS; j++){
                cost=cplex.sum(cost,cplex.prod(energy[i][j],prices[(i+startTime)%24][j]));
            }
        }
        cplex.addMinimize(cost);

        //set constraint
        for(int i=0;i<length;i++){
            //the input and output power should not exceed the maximum input and output power
            cplex.addLe(-normalBattery.getMaxIOPower().doubleValue(),cplex.sum(realTimeEnergy[i+1],cplex.negative(realTimeEnergy[i])));
            cplex.addLe(cplex.sum(realTimeEnergy[i+1],cplex.negative(realTimeEnergy[i])),normalBattery.getMaxIOPower().doubleValue());
        }

        //start energy is 0
        cplex.addEq(realTimeEnergy[0],startBatteryEnergy);


        for(int i=0;i<length;i++){
            //the sum of energy at the time interval should be equal to the load plus the energy import or export from the battery
            cplex.addEq(cplex.sum(energy[i][NORMAL],energy[i][PEAK]),
                    cplex.sum(loads[i+startTime],cplex.sum(realTimeEnergy[i+1],cplex.negative(realTimeEnergy[i]))));
            //the energy at non-peak price level should not exceed the peak limit
            cplex.addLe(energy[i][NORMAL], PEAK_LIMIT);
            for(int j = 0; j< LOAD_LEVELS; j++){
                cplex.addGe(energy[i][j],0);
            }
        }


        //solve the problem
        double currentEnergy=0;
        if(cplex.solve()){
            currentEnergy=cplex.getValue(realTimeEnergy[1]);
            for(int i=0;i<length;i++){
                for(int j = 0; j< LOAD_LEVELS; j++){
                    realEnergy[i+startTime][j]= cplex.getValue(energy[i][j]);
                }
            }
        }
        cplex.endModel();
        cplex.close();

        return currentEnergy;


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
        realEnergy=new double[timeInterval][LOAD_LEVELS];
    }

    @Override
    public void run() {
        double currentEnergy = 0;

        //windows move from the beginning to the end and traverses all the time intervals (except for the last mu intervals)
        //each time fix the operation (solution) at that time interval
        for(int t = 0; t<=timeInterval- MU; t++){
            try {
                currentEnergy=MILP(MU,t,startEnergy);
            } catch (IloException e) {
                e.printStackTrace();
            }

            startEnergy=currentEnergy;
        }

    }


    @Override
    public void setLoop(int loop) {
        this.loop=loop;
    }

    @Override
    public BigDecimal findMin() {
        double cost=0;
        for(int i=0;i<timeInterval;i++){
            for(int j = 0; j< LOAD_LEVELS; j++){
                cost+=realEnergy[i][j]*prices[i%24][j];
            }
        }
        return new BigDecimal(cost).setScale(3,RoundingMode.HALF_UP);
    }

    @Override
    public void printPath() {

    }

    double[] listToArray(List<BigDecimal> subproblem) {
        double[] loads=new double[subproblem.size()];
        int i=0;
        for(BigDecimal f:subproblem){
            loads[i]=f.setScale(3,RoundingMode.HALF_UP).doubleValue();
            i++;
        }
        return loads;
    }

    int roundNextMulti(int problemSize, int mu) {
        while(problemSize>=0){
            if(problemSize%mu==0){
                break;
            }
            problemSize=problemSize-1;
        }

        return problemSize;
    }

    @Override
    public void reset(){
        startEnergy = 0;
        timeInterval=loads.length;
        realEnergy=new double[timeInterval][LOAD_LEVELS];
        normalBattery.setCurrentEnergy(new BigDecimal(0));
    }
}
