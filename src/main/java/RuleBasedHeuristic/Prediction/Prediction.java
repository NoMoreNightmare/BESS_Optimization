package RuleBasedHeuristic.Prediction;

public enum Prediction {
    //previous information is not available
    NO_PREV_INFO,
    //charge operation
    CHARGE,
    //discharge operation for peak situation
    PEAK_DISCHARGE,
    //discharge operation for non-peak situation
    NON_PEAK_DISCHARGE,
    //limited charge
    LIMITED_CHARGE,
    //limited discharge
    LIMITED_DISCHARGE,
    //don't charge or discharge
    NO_OPERATION,
}
