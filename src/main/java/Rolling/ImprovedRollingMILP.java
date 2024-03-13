package Rolling;
import Battery.Battery;
import Battery.NormalBattery;
import Interface.Controllable;
import Model.LoadData;
import com.opencsv.exceptions.CsvException;

import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * the improved rolling horizon approach based on MILP
 */
public class ImprovedRollingMILP implements Controllable {
    int timeInterval=24;
    Battery normalBattery;

    double[] loads;
    final int MU =48;
    final int TARIFF_LENGTH =24;
    final double EPSILON =0.1;

    final int LOAD_LEVELS =2;
    double[][] prices=new double[TARIFF_LENGTH][LOAD_LEVELS];


    final double PEAK_LIMIT =0.08;


    int loop;

    double startEnergy=0;

    double[][] tempEnergy;
    double[][] realEnergy;

    String filename;

    class Cell{
        public double currentCost;
        public double[] currentEnergy;

        public Cell(double cost,double[] energy){
            currentCost=cost;
            currentEnergy=energy;
        }

    }

    public ImprovedRollingMILP(BigDecimal maxBatteryEnergy, String filename)  {
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

    public Cell MILP(int length,int startTime,double startBatteryEnergy) throws IloException {
        //build the model

        IloCplex cplex=new IloCplex();
        cplex.setOut(OutputStream.nullOutputStream());

        IloNumVar[][] energy=new IloNumVar[length][];
        for(int i=0;i<length;i++){
            energy[i]=cplex.numVarArray(LOAD_LEVELS,0,Double.MAX_VALUE);
        }

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
            cplex.addLe(-normalBattery.getMaxIOPower().doubleValue(),cplex.sum(realTimeEnergy[i+1],cplex.negative(realTimeEnergy[i])));
            cplex.addLe(cplex.sum(realTimeEnergy[i+1],cplex.negative(realTimeEnergy[i])),normalBattery.getMaxIOPower().doubleValue());
        }

        cplex.addEq(realTimeEnergy[0],startBatteryEnergy);


        for(int i=0;i<length;i++){
            cplex.addEq(cplex.sum(energy[i][NORMAL],energy[i][PEAK]),
                    cplex.sum(loads[i+startTime],cplex.sum(realTimeEnergy[i+1],cplex.negative(realTimeEnergy[i]))));
            cplex.addLe(energy[i][NORMAL], PEAK_LIMIT);
            for(int j = 0; j< LOAD_LEVELS; j++){
                cplex.addGe(energy[i][j],0);
            }
        }


        //solve the problem
        double currentOptima=0;
        double[] currentEnergy = new double[0];
        if(cplex.solve()){
            currentEnergy=new double[energy.length];
            currentOptima=cplex.getObjValue();
            for(int i=0;i<length;i++){
                for(int j = 0; j< LOAD_LEVELS; j++){
                    tempEnergy[i+startTime][j]= cplex.getValue(energy[i][j]);
                };
                currentEnergy[i]=cplex.getValue(realTimeEnergy[i + 1]);
            }
        }
        cplex.endModel();
        cplex.close();

        return new Cell(currentOptima,currentEnergy);


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
        tempEnergy=new double[timeInterval][LOAD_LEVELS];


        realEnergy=new double[timeInterval][LOAD_LEVELS];
        for (int i = 0; i < realEnergy.length; i++) {
            Arrays.fill(realEnergy[i],Double.MAX_VALUE);
        }
    }

    @Override
    public void run() {
        int beta=timeInterval-roundNextMulti(timeInterval-1, MU);
        int start=0;
        double currentLocalOptima;


        //solve subproblem (0...beta-1) and get optimal
        Cell cell = null;
        try {
            cell=MILP(beta,start,startEnergy);
        } catch (IloException e) {
            e.printStackTrace();
        }
        currentLocalOptima=cell.currentCost;
        //store the solution of the subproblem (0...beta-1)
        assignValue(0,beta);

        //the position of the current battery energy
        int key=0;
        for(int t=0;t<=beta;t++){
            //store the previous solution's information
            Cell oldCell=new Cell(cell.currentCost,cell.currentEnergy);
            try {
                //solve new subproblem within the new window
                cell=MILP(MU,t,startEnergy);
            } catch (IloException e) {
                e.printStackTrace();
            }

            //calculate the cost of new solution for the fixed subproblem
            double newCost=0;
            for(int i=0;i<t;i++){
                newCost=newCost+realEnergy[i][NORMAL]*prices[i%24][NORMAL]+realEnergy[i][PEAK]*prices[i%24][PEAK];
            }
            for(int i=t;i<beta;i++){
                newCost=newCost+tempEnergy[i][NORMAL]*prices[i%24][NORMAL]+tempEnergy[i][PEAK]*prices[i%24][PEAK];

            }

            //compare the cost carrying influence of new solution with old cost
            if(newCost<=(EPSILON +1)*currentLocalOptima){
                key=0;
                assignValue(t, MU);
                startEnergy=cell.currentEnergy[key];
            }else{
                //if the solution is empty, store the new solution by no means
                if (realEnergy[t][NORMAL]==Double.MAX_VALUE){
                    assignValue(t, MU);
                    key=0;
                    startEnergy=cell.currentEnergy[key];
                    continue;
                }
                //the new solution is dropped, but the window has to move
                cell=oldCell;
                startEnergy=cell.currentEnergy[key];
                //get the next day's battery energy
                key+=1;
            }
        }




        //judge the number of the loop according to whether the size of the problem can be divided by the window size
        int nums;
        if(timeInterval% MU ==0){
            nums=timeInterval/ MU -2;
        }else{
            nums=timeInterval/ MU -1;
        }

        //repeat and traverse the solving process
        for(int num=0;num<nums;num++){
            for(int t = beta+num* MU; t<=beta+(num+1)* MU; t++){
                Cell oldCell=new Cell(cell.currentCost,cell.currentEnergy);
                try {
                    cell=MILP(MU,t,startEnergy);
                } catch (IloException e) {
                    e.printStackTrace();
                }

                double newCost=0;
                for(int i = beta+num* MU; i<t; i++){
                    newCost=newCost+realEnergy[i][NORMAL]*prices[i%24][NORMAL]+realEnergy[i][PEAK]*prices[i%24][PEAK];
                }
                for(int i = t; i<beta+(num+1)* MU; i++){
                    newCost=newCost+tempEnergy[i][NORMAL]*prices[i%24][NORMAL]+tempEnergy[i][PEAK]*prices[i%24][PEAK];

                }
                if(newCost<=(EPSILON +1)*currentLocalOptima){
                    key=0;
                    assignValue(t, MU);
                    startEnergy=cell.currentEnergy[key];
                }else{
                    if (realEnergy[t][NORMAL]==Double.MAX_VALUE){//如果realenergy[time]没用内容，则赋值
                        assignValue(t, MU);
                        key=0;
                        startEnergy=cell.currentEnergy[key];
                        continue;
                    }
                    cell=oldCell;
                    startEnergy=cell.currentEnergy[key];
                    key+=1;
                }
            }
        }


    }

    public void assignValue(int start, int length) {
        for(int i=start;i<start+length;i++){
            realEnergy[i][NORMAL]=tempEnergy[i][NORMAL];
            realEnergy[i][PEAK]=tempEnergy[i][PEAK];
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
        tempEnergy=new double[timeInterval][LOAD_LEVELS];
        realEnergy=new double[timeInterval][LOAD_LEVELS];
        normalBattery.setCurrentEnergy(new BigDecimal(0));
    }
}
