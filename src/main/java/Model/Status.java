package Model;

/**
 * used for DP and rule-based heuristic to indicate the status of current tariff structure and load
 */
public enum Status {
    //exceed the peak limit
    PEAK,
    //the price is at a high level
    HIGH_PRICE,
    //the price is at a normal level
    NORMAL_PRICE,
    //the price is at a low level
    LOW_PRICE
}
