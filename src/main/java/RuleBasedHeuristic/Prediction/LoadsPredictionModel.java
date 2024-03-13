package RuleBasedHeuristic.Prediction;

import Model.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * the prediction component
 */
public class LoadsPredictionModel {
    BigDecimal[] loads;
    BigDecimal peakLimit;
    private final int TARIFF_LENGTH=24;

    private final String CLOSE_THRESHOLD="0.8";
    private final String SIMILAR_THRESHOLD="0.4";
    private final String LOW_THRESHOLD="0.5";

    public LoadsPredictionModel(BigDecimal[] loads,BigDecimal peakLimit){
        this.loads=loads;
        this.peakLimit=peakLimit;
    }

    public Prediction predict(int timeInterval, Status status){

        // with no previous information
        if(timeInterval<TARIFF_LENGTH){
            return Prediction.NO_PREV_INFO;
        }else{
            // the load exceed the peak limit
            if(status==Status.PEAK){
                return Prediction.PEAK_DISCHARGE;
            }else {
                // the price is normal, the model should charge limited
                if(status==Status.NORMAL_PRICE){
                    return Prediction.LIMITED_CHARGE;
                // the price is cheap, then the heuristic should charge
                }else if(status==Status.LOW_PRICE){
                    return Prediction.CHARGE;
                }else{// now the price level is high
                    //if the load is similar to previous same position of the data, the trend may be similar
                    if(isSimilar(timeInterval)){
                        //observe two loads next to previous load
                        //if both of them exceeds the peak limit, then it is possible that the
                        //          load will exceed the peak limit at the same time, hence do nothing
                        //else if only one of them exceed the peak limit, discharge limitedly at current time
                        //                                to maximum utilize the energy in the battery
                        //else if none of them exceeds the peak limit, then if current load
                        //      approaches the peak limit, it is more likely the future load will exceed
                        //      only limited discharge is allowed
                        //     otherwise, discharge as much as it can
                        if(exceedPeak(timeInterval-23)){
                            if(exceedPeak(timeInterval-22)){
                                return Prediction.NO_OPERATION;
                            }
                            return Prediction.LIMITED_DISCHARGE;
                        }else if(!exceedPeak(timeInterval-22)){
                            if(isCloseTo(timeInterval)){
                                return Prediction.LIMITED_DISCHARGE;
                            }else{
                                return Prediction.NON_PEAK_DISCHARGE;
                            }
                        }else{
                            return Prediction.LIMITED_DISCHARGE;
                        }

                    }else{// now current load and previous load has no similarity
                        // if current load is low, the future load may be low too. Hence, discharge as much as it can
                        if(isLoadLow(timeInterval)){
                            return Prediction.NON_PEAK_DISCHARGE;
                        }else{//the load is not low
                            //if it is closed to peak limit, don't discharge
                            // else limited discharge
                            if(isCloseTo(timeInterval)){
                                return Prediction.NO_OPERATION;
                            }else{
                                return Prediction.LIMITED_DISCHARGE;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isCloseTo(int timeInterval) {
        return loads[timeInterval].doubleValue()>=peakLimit.multiply(new BigDecimal(CLOSE_THRESHOLD).setScale(8, RoundingMode.HALF_UP)).doubleValue();
    }

    private boolean isSimilar(int timeInterval){
        return (loads[timeInterval].subtract(loads[timeInterval-24])).doubleValue()<peakLimit.multiply(new BigDecimal(SIMILAR_THRESHOLD).setScale(8, RoundingMode.HALF_UP)).doubleValue();
    }

    private boolean exceedPeak(int timeInterval){
        return loads[timeInterval].doubleValue()>peakLimit.doubleValue();
    }

    private boolean isLoadLow(int timeInterval){
        return loads[timeInterval].doubleValue()<=peakLimit.multiply(new BigDecimal(LOW_THRESHOLD).setScale(8, RoundingMode.HALF_UP)).doubleValue();
    }


}
