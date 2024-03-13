package Model;

/**
 * the data structure store the results for different algorithms
 */
public class Result{
    private double dp;

    private double pruneddp;

    private double rollingdp;

    private double rollingmilp;

    private double ruleH;

    private double ruleHPredict;


    public double getDp() {
        return dp;
    }

    public void setDp(double dp) {
        this.dp = dp;
    }

    public double getPruneddp() {
        return pruneddp;
    }

    public void setPruneddp(double pruneddp) {
        this.pruneddp = pruneddp;
    }

    public double getRollingdp() {
        return rollingdp;
    }

    public void setRollingdp(double rollingdp) {
        this.rollingdp = rollingdp;
    }

    public double getRollingmilp() {
        return rollingmilp;
    }

    public void setRollingmilp(double rollingmilp) {
        this.rollingmilp = rollingmilp;
    }

    public double getRuleH() {
        return ruleH;
    }

    public void setRuleH(double ruleH) {
        this.ruleH = ruleH;
    }

    public double getRuleHPredict() {
        return ruleHPredict;
    }

    public void setRuleHPredict(double ruleHPredict) {
        this.ruleHPredict = ruleHPredict;
    }
}