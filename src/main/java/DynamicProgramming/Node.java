package DynamicProgramming;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * the state in the DP
 */
public class Node {
    /**
     * the cost from the beginning to current state
     */
    public BigDecimal cost;
    /**
     * flags for whether the state is reachable
     */
    public boolean isAvailable;
    /**
     * the node that can reach with minimum cost
     */
    public Node before;
    /**
     *  the remaining battery energy in the state
     */
    public BigDecimal energy;
    public Node(BigDecimal energy){
        cost=new BigDecimal(Double.MAX_VALUE).setScale(8, RoundingMode.HALF_UP);
        isAvailable=false;
        before=null;
        this.energy =energy;
    }

    public void reset(){
        cost=new BigDecimal(Double.MAX_VALUE).setScale(8, RoundingMode.HALF_UP);
        isAvailable=false;
        before=null;

    }
}
