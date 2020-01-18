package edu.neu.ccs.wellness.fitness;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.neu.ccs.wellness.fitness.interfaces.MultiDayFitnessInterface;
import edu.neu.ccs.wellness.fitness.interfaces.OneDayFitnessInterface;

/**
 * Created by hermansaksono on 3/20/18.
 */

public class MultiDayFitness implements MultiDayFitnessInterface {

    // CONSTANTS
    public static final int MIN_NUM_STEPS = 1000;

    //PRIVATE MEMBERS
    private Date startDate;
    private Date endDate;
    private int numberOfDays;
    private int elapsedDays;
    private List<OneDayFitnessInterface> dailyFitness;

    public MultiDayFitness(Date startDate, Date endDate, int numberOfDays, int elapsedDays,
                           List<OneDayFitnessInterface> oneDayFitnessInterfaces){
        this.startDate = startDate;
        this.endDate = endDate;
        this.numberOfDays = numberOfDays;
        this.elapsedDays = elapsedDays;
        this.dailyFitness = oneDayFitnessInterfaces;
    }

    public static MultiDayFitness newInstance(Date startDate, Date endDate,
                                              int numberOfDays, int elapsedDays,
                                              List<OneDayFitnessInterface> dailyFitness ){
        return new MultiDayFitness(startDate, endDate, numberOfDays, elapsedDays, dailyFitness);
    }

    public static MultiDayFitness newInstance(Date startDate, Date endDate,
                                              List<OneDayFitnessInterface> dailyFitness ){
        int numDays = getNumDays(startDate, endDate);
        int elapsedDays = getElapsedDays(startDate);
        return new MultiDayFitness(startDate, endDate, numDays, elapsedDays, dailyFitness);
    }

    @Override
    public Date getStartDate() {
        return this.startDate;
    }

    @Override
    public Date getEndDate() {
        return this.endDate;
    }

    @Override
    public int getNumDays() {
        return this.numberOfDays;
    }

    @Override
    public int getElapsedDays() {
        return this.elapsedDays;
    }

    @Override
    public List<OneDayFitnessInterface> getDailyFitness() {
        return this.dailyFitness;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (OneDayFitnessInterface oneDay : dailyFitness) {
            sb
                    .append("\t")
                    .append(oneDay.toString())
                    .append("\n");
        }
        return sb.toString();
    }

    public int getStepsAverage() {
        float totalSteps = 0f;
        int totalDays = 0;
        for (OneDayFitnessInterface oneDayFitness : this.getDailyFitness()) {
            totalSteps += oneDayFitness.getSteps();
            totalDays += 1;
        }

        if (totalDays == 0) {
            return MIN_NUM_STEPS;
        } else {
            return Math.max(Math.round(totalSteps / totalDays), MIN_NUM_STEPS);
        }
    }

    /* DATE HELPER METHODS */
    private static int getNumDays(Date startDate, Date endDate) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);

        return getDifferencesInDays(startCal, endCal);
    }

    private static int getElapsedDays(Date startDate) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);

        Calendar endCal = Calendar.getInstance();

        return getDifferencesInDays(startCal, endCal);
    }

    private static int getDifferencesInDays(Calendar startCal, Calendar endCal) {
        long interval = endCal.getTimeInMillis() - startCal.getTimeInMillis();
        return (int) TimeUnit.MILLISECONDS.toDays((long) Math.floor(interval));
    }
}
