package MILP;

import Battery.*;
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

public class MILP implements Controllable {
    double optimal=0;
    int timeInterval;
    int LOAD_LEVELS=2;
    int TARIFF_LENGTH=24;
    Battery normalBattery;
    double startBatteryEnergy=0;
    double[] loads;
    double PEAK_LIMIT=0.08;
    double[][] prices;
    String filename;
    int loop;
    public MILP(BigDecimal maxBatteryEnergy,String filename){
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
        prices=new double[timeInterval][LOAD_LEVELS];
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

    @Override
    public void run() throws IloException {
        IloCplex cplex=new IloCplex();
        cplex.setOut(OutputStream.nullOutputStream());

        //define parameters 只有decision variables需要，其他的直接用java数组的形式定义和参与计算
        IloNumVar[][] energy=new IloNumVar[timeInterval][];
        for(int i=0;i<timeInterval;i++){
            //the energy at each time interval and different part should be greater or equal to 0
            energy[i]=cplex.numVarArray(LOAD_LEVELS,0,Double.MAX_VALUE);
        }

        //the battery energy should be less than the battery capacity and greater than 0
        IloNumVar[] realTimeEnergy=cplex.numVarArray(timeInterval+1,0,normalBattery.getMaxBatteryEnergy().setScale(2, RoundingMode.HALF_UP).doubleValue());


        //set objective function
        IloNumExpr cost=cplex.numExpr();
        for(int i=0;i<timeInterval;i++){
            for(int j = 0; j< LOAD_LEVELS; j++){
                cost=cplex.sum(cost,cplex.prod(energy[i][j],prices[i%24][j]));
            }
        }
        cplex.addMinimize(cost);

        //set constraint
        for(int i=0;i<timeInterval;i++){
            //the input and output power should not exceed the maximum input and output power
            cplex.addLe(-normalBattery.getMaxIOPower().doubleValue(),cplex.sum(realTimeEnergy[i+1],cplex.negative(realTimeEnergy[i])));
            cplex.addLe(cplex.sum(realTimeEnergy[i+1],cplex.negative(realTimeEnergy[i])),normalBattery.getMaxIOPower().doubleValue());
        }

        //start energy is 0
        cplex.addEq(realTimeEnergy[0],startBatteryEnergy);


        for(int i=0;i<timeInterval;i++){
            //the sum of energy at the time interval should be equal to the load plus the energy import or export from the battery
            cplex.addEq(cplex.sum(energy[i][NORMAL],energy[i][PEAK]),
                    cplex.sum(loads[i],cplex.sum(realTimeEnergy[i+1],cplex.negative(realTimeEnergy[i]))));
            //the energy at non-peak price level should not exceed the peak limit
            cplex.addLe(energy[i][NORMAL], PEAK_LIMIT);
            for(int j = 0; j< LOAD_LEVELS; j++){
                cplex.addGe(energy[i][j],0);
            }
        }


        //solve the problem
        if(cplex.solve()){
            optimal=cplex.getObjValue();
        }
        cplex.endModel();
        cplex.close();

    }

    @Override
    public void setLoop(int loop) {
        this.loop=loop;
    }

    @Override
    public BigDecimal findMin() {
        return BigDecimal.valueOf(optimal).setScale(3,RoundingMode.HALF_UP);
    }

    @Override
    public void printPath() {

    }

    @Override
    public void reset() {
        normalBattery.setCurrentEnergy(new BigDecimal(0));
    }
}
