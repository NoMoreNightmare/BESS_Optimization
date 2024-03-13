package Model;

/**
 * the data structure store the results for different hyperparameter value
 */
public class ResultForParameter {
    /**
     * window size is 24
     */
    private double mu24;

    private double mu48;

    private double mu336;

    private double mu1440;



    public double getMu24() {
        return mu24;
    }

    public void setMu24(double mu24) {
        this.mu24 = mu24;
    }

    public double getMu48() {
        return mu48;
    }

    public void setMu48(double mu48) {
        this.mu48 = mu48;
    }

    public double getMu336() {
        return mu336;
    }

    public void setMu336(double mu336) {
        this.mu336 = mu336;
    }

    public double getMu1440() {
        return mu1440;
    }

    public void setMu1440(double mu1440) {
        this.mu1440 = mu1440;
    }

}
