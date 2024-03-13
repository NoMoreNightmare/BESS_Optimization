package Interface;

import com.opencsv.exceptions.CsvException;
import ilog.concert.IloException;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * the interface of all algorithms: with all the methods the algorithms need to implement
 */
public interface Controllable {
    public final int NORMAL=0;
    public final int PEAK=1;


    /**
     * load data
     * @throws IOException the exception when reading files
     * @throws CsvException th exception when dealing with csv file
     */
    void loadData() throws IOException, CsvException;

    /**
     * the entry of the algorithm
     * @throws IloException if call cplex, then throw this exception
     */
    void run() throws IloException;

    /**
     * the number of times of running algorithms
     * no longer used
     * @param loop
     */
    @Deprecated
    void setLoop(int loop);

    /**
     * return the global solution value
     * @return the minimum cost
     */
    BigDecimal findMin();

    /**
     * when try to determine the bugs, use this approach to print the series of operations and its path
     */
    void printPath();

    /**
     * reset the algorithms
     */
    void reset();

    /**
     * the energy and cost at each state
     */
    class SoCCost{
        public BigDecimal soc;
        public BigDecimal cost;

        public SoCCost(BigDecimal soc,BigDecimal cost){
            this.soc=soc;
            this.cost=cost;
        }
    }
}
