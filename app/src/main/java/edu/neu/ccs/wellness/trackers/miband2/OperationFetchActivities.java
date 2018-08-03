package edu.neu.ccs.wellness.trackers.miband2;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.neu.ccs.wellness.trackers.callback.FetchActivityListener;
import edu.neu.ccs.wellness.trackers.callback.NotifyListener;
import edu.neu.ccs.wellness.trackers.miband2.model.Profile;
import edu.neu.ccs.wellness.trackers.miband2.model.Protocol;
import edu.neu.ccs.wellness.trackers.miband2.utils.CalendarUtils;
import edu.neu.ccs.wellness.trackers.miband2.utils.TypeConversionUtils;

/**
 * Created by hermansaksono on 6/22/18.
 */

public class OperationFetchActivities {

    private static final int BTLE_DELAY_MODERATE = 1000;
    private static final int BTLE_WAIT_FOR_ALL_SAMPLES = 2000;
    private static final int BTLE_DELAY_LONG = 3000;
    private static final int ONE_MIN_ARRAY_SUBSET_LENGTH = 4;
    private static final int STEPS_DATA_INDEX = 3;
    private static final int NUM_PACKETS_INTEGRITY = 1;
    private static final String TAG = "mi-band-activities";

    private BluetoothIO io;
    private GregorianCalendar startDate;
    private Handler handler;
    private int expectedNumberOfSamples;
    private int expectedNumberOfPackets;
    private List<List<Integer>> rawPackets;
    private List<Integer> fitnessSamples;
    private FetchActivityListener fetchActivityListener;

    private NotifyListener notifyListener = new NotifyListener() {
        @Override
        public void onNotify(byte[] data) {
            processRawActivityData(data);
        }
    };
    private Runnable packetsWaitingRunnable;

    public OperationFetchActivities(FetchActivityListener notifyListener, Handler handler) {
        this.fetchActivityListener = notifyListener;
        this.handler = handler;
    }

    public void perform(BluetoothIO io, GregorianCalendar date) {
        Calendar expectedEndDate = CalendarUtils.getRoundedMinutes(GregorianCalendar.getInstance());
        expectedEndDate.add(Calendar.MINUTE, -1);
        this.io = io;
        this.startDate = date;
        this.expectedNumberOfSamples = (int) CalendarUtils.getDurationInMinutes(date, expectedEndDate);
        this.expectedNumberOfPackets = (int) Math.ceil(this.expectedNumberOfSamples / 4f);
        this.rawPackets = new ArrayList<>();

        Log.v(TAG, String.format("Expecting to stop after %d samples, %d packets",
                expectedNumberOfSamples, expectedNumberOfPackets));
        startFetchingFitnessData();
    }

    private void startFetchingFitnessData() {
        this.io.stopNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_5_ACTIVITY);
        this.enableFetchUpdatesNotify();
    }

    private void enableFetchUpdatesNotify() { // This needs delay
        this.io.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_4_FETCH, new NotifyListener() {
        @Override
        public void onNotify(byte[] data) {
            Log.d(TAG, Arrays.toString(data));
            if (data[1] == 1) {         // [16, 1, 1, 5, 0, 0, 0, -30, 7, 8, 3, 14, 31, 0, -16]
                startNotifyingFitnessData();
            } else if (data[1] == 2) {  // [16, 2, 1]
                completeFetchingProcess();
                handler.removeCallbacks(packetsWaitingRunnable);
            }
        }
        });
        this.handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendCommandParams();
            }
        }, BTLE_DELAY_MODERATE);
    }

    private void sendCommandParams() { // This doesn't need delay
        byte[] params = getFetchingParams(startDate);
        Log.d(TAG, String.format("Fetching fitness data from %s.", startDate.getTime().toString()));
        Log.v(TAG, String.format("Fetching fitness params: %s", Arrays.toString(getFetchingParams(startDate))));
        this.io.writeCharacteristic(Profile.UUID_CHAR_4_FETCH, params, null);
        this.enableFitnessDataNotify();
    }

    private void enableFitnessDataNotify() { // This doesn't need delay
        this.io.setNotifyListener(
                Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_5_ACTIVITY, this.notifyListener);
        /*
        this.handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startNotifyingFitnessData();
            }
        }, BTLE_DELAY_MODERATE);
        */
    }

    private void startNotifyingFitnessData() {
        this.io.writeCharacteristic(
                Profile.UUID_CHAR_4_FETCH, Protocol.COMMAND_ACTIVITY_FETCH, null);
    }

    /* PARAM METHODS */
    private static byte[] getFetchingParams(Calendar startDate) {
        byte[] paramStartTime = TypeConversionUtils.getTimeBytes(startDate, TimeUnit.MINUTES);
        return TypeConversionUtils.join(Protocol.COMMAND_ACTIVITY_PARAMS, paramStartTime);
    }

    /* ACTIVITY DATA PROCESSING METHODS */
    private void processRawActivityData(byte[] data) {
        rawPackets.add(Arrays.asList(TypeConversionUtils.byteArrayToIntegerArray(data)));
        Log.v(TAG, String.format("Fitness packet %d: %s", rawPackets.size(), Arrays.toString(data)));

        if (rawPackets.size() == NUM_PACKETS_INTEGRITY) {
            waitAndComputeSamples(rawPackets.size());
        }
    }

    private void waitAndComputeSamples(final int numSamplesPreviously) {
        /*
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (rawPackets.size() >= expectedNumberOfPackets) {
                    Log.v(TAG, String.format("Completed fetching %d/%d packets",
                            rawPackets.size(), expectedNumberOfPackets ));
                    fitnessSamples = getFitnessSamplesFromRawPackets(rawPackets);
                    notifyFetchListener();
                } else if (rawPackets.size() > numSamplesPreviously) {
                    Log.v(TAG, String.format("Continue fetching after %d/%d packets",
                            rawPackets.size(), expectedNumberOfPackets ));
                    waitAndComputeSamples(rawPackets.size());
                } else {
                    Log.d(TAG, String.format("Aborting fetch after %d/%d packets",
                            rawPackets.size(), expectedNumberOfPackets ));
                    fitnessSamples = getFitnessSamplesFromRawPackets(rawPackets);
                    notifyFetchListener();
                }
            }
        }, BTLE_WAIT_FOR_ALL_SAMPLES);
        */
        this.packetsWaitingRunnable = new Runnable() {
            @Override
            public void run() {
                if (rawPackets.size() > numSamplesPreviously) {
                    Log.v(TAG, String.format("Continue fetching after %d/%d packets",
                            rawPackets.size(), expectedNumberOfPackets ));
                    waitAndComputeSamples(rawPackets.size());
                } else {
                    Log.d(TAG, String.format("Aborting fetch after %d/%d packets",
                            rawPackets.size(), expectedNumberOfPackets ));
                    completeFetchingProcess();
                }
            }
        };

        handler.postDelayed(this.packetsWaitingRunnable, BTLE_WAIT_FOR_ALL_SAMPLES);
    }

    private void completeFetchingProcess() {
        this.fitnessSamples = getFitnessSamplesFromRawPackets(rawPackets);
        if (fetchActivityListener != null) {
            fetchActivityListener.OnFetchComplete(this.startDate, this.fitnessSamples);
        }
        this.io.stopNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_5_ACTIVITY);
    }

    private void notifyFetchListener() {
        if (fetchActivityListener != null) {
            fetchActivityListener.OnFetchComplete(this.startDate, this.fitnessSamples);
        }
        this.io.stopNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_5_ACTIVITY);
    }

    /* FITNESS SAMPLES METHODS */
    private static List<Integer> getFitnessSamplesFromRawPackets(List<List<Integer>> rawSamples) {
        List<Integer> fitnessSamples = new ArrayList<>();
        for (List<Integer> rawSample : rawSamples) {
            fitnessSamples.add(getSteps(rawSample, 0));
            fitnessSamples.add(getSteps(rawSample, 1));
            fitnessSamples.add(getSteps(rawSample, 2));
            fitnessSamples.add(getSteps(rawSample, 3));
        }
        return fitnessSamples;
    }

    private static int getSteps(List<Integer> rawSample, int subindex) {
        int rawSampleIndex = (subindex * ONE_MIN_ARRAY_SUBSET_LENGTH) + STEPS_DATA_INDEX;
        if (rawSampleIndex < rawSample.size()) {
            return rawSample.get(rawSampleIndex);
        } else {
            return 0;
        }
    }
}
