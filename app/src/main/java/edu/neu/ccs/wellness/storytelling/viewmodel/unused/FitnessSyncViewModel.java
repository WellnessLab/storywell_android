package edu.neu.ccs.wellness.storytelling.viewmodel.unused;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import edu.neu.ccs.wellness.fitness.storage.FitnessRepository;
import edu.neu.ccs.wellness.fitness.storage.onDataUploadListener;
import edu.neu.ccs.wellness.trackers.miband2.ActionCallback;
import edu.neu.ccs.wellness.trackers.miband2.MiBand;
import edu.neu.ccs.wellness.trackers.miband2.listeners.FetchActivityListener;
import edu.neu.ccs.wellness.people.Group;
import edu.neu.ccs.wellness.people.Person;
import edu.neu.ccs.wellness.storytelling.sync.SyncStatus;
import edu.neu.ccs.wellness.storytelling.utils.StorywellPerson;
import edu.neu.ccs.wellness.utils.WellnessDate;

/**
 * Created by hermansaksono on 7/19/18.
 */

public class FitnessSyncViewModel extends AndroidViewModel {

    private static final int SAFE_MINUTES = 5;

    private MutableLiveData<SyncStatus> status = null;

    private MiBand miBand;
    private List<StorywellPerson> storywellMembers;
    private List<StorywellPerson> btPersonQueue = new Vector<>();
    private Map<StorywellPerson, BluetoothDevice> foundBluetoothDeviceList = new HashMap<>();

    private StorywellPerson currentPerson = null;

    /* VARIABLES FOR UPLOADING DATA */
    FitnessRepository fitnessRepository;


    /* CONSTRUCTOR*/
    public FitnessSyncViewModel(@NonNull Application application) {
        super(application);
        this.fitnessRepository = new FitnessRepository();
    }

    /* PUBLIC METHODS*/
    /**
     * Connect to fitness trackers, download the data from the tracker, and upload it to the
     * repository. These steps are performed to each of the members of Group. The data must be
     * downloaded starting from startDate that is unique to every user.
     * @param group
     * @return
     */
    public LiveData<SyncStatus> perform(Group group) {
        this.miBand = new MiBand(this.getApplication());
        this.storywellMembers = getStorywellMembers(group);

        if (this.status == null) {
            this.status = new MutableLiveData<>();
            this.status.setValue(SyncStatus.UNINITIALIZED);
        }
        MiBand.startScan(getScanCallback());
        Log.d("SWELL", "Starting to look for fitness trackers");
        return this.status;
    }

    /**
     * Perform synchronization on the next person in queue.
     */
    public boolean performNext () {
        if (this.status == null) {
            return false;
        } else {
            this.miBand = new MiBand(this.getApplication());
            this.connectFromQueue(this.btPersonQueue);
            return true;
        }
    }

    /**
     * Get the person whose data is currently being fetched.
     * @return @StorywellPerson Person who is currently being synchronized.
     */
    public StorywellPerson getCurrentPerson() {
        return this.currentPerson;
    }

    /* BLUETOOTH SCAN CALLBACK */
    private ScanCallback getScanCallback() {
        return new ScanCallback() {
            int numDevices = 0;

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();

                if (tryAddDevice(device)) {
                    numDevices += 1;
                }

                if (numDevices == storywellMembers.size()) {
                    MiBand.stopScan(this);
                }
            }
        };
    }

    private boolean tryAddDevice(BluetoothDevice device) { // TODO This is not optimal O(n)
        for (StorywellPerson person: this.storywellMembers) {
            String address = person.getBtProfile().getAddress();
            if (address.equals(device.getAddress())) {
                this.foundBluetoothDeviceList.put(person, device);
                this.btPersonQueue.add(person);

                if (person.getPerson().isRole(Person.ROLE_PARENT)) {
                    connectFromQueue(this.btPersonQueue);
                }
                return true;
            }
        }
        return false;
    }

    /* BLUETOOTH CONNECTION METHODS */
    private void connectFromQueue(List<StorywellPerson> queue) {
        Log.d("SWELL", "Bluetooth Connection Queue: " + Arrays.toString(queue.toArray()));
        if (queue.size() > 0) {
            this.currentPerson = queue.get(0);
            queue.remove(0);
            this.connectToMiBand(this.foundBluetoothDeviceList.get(this.currentPerson), this.currentPerson);
        } else {
            this.status.setValue(SyncStatus.SUCCESS);
        }
    }

    /**
     * INVARIANT: The device has been been authenticated for this Android device.
     * @param device The device to connect to.
     * @param person
     */
    private void connectToMiBand(BluetoothDevice device, final StorywellPerson person) {
        this.miBand.connect(device, new ActionCallback() {
            @Override
            public void onSuccess(Object data){
                doPair(person);
            }

            @Override
            public void onFail(int errorCode, String msg){
                Log.e("SWELL", String.format("Connect failed (%d): %s", errorCode, msg));
            }
        });
        this.status.setValue(SyncStatus.CONNECTING);
    }

    /* PAIRING METHODS */
    private void doPair(final StorywellPerson person) {
        this.miBand.pair(new ActionCallback() {
            @Override
            public void onSuccess(Object data){
                doPostPair(person);
                Log.d("SWELL", String.format("Paired: %s", data.toString()));
            }
            @Override
            public void onFail(int errorCode, String msg){
                Log.e("SWELL", String.format("Pair failed (%d): %s", errorCode, msg));
            }
        });
    }

    private void doPostPair(StorywellPerson person) {
        this.doDownloadFromBand(person);
    }

    /* DOWNLOADING METHODS */
    private void doDownloadFromBand(final StorywellPerson person) {
        GregorianCalendar startDate = person.getLastSyncTime(this.getApplication());
        this.status.postValue(SyncStatus.DOWNLOADING);
        this.miBand.fetchActivityData(startDate, new FetchActivityListener() {
            @Override
            public void OnFetchComplete(Calendar startDate, List<Integer> steps) {
                doUploadToRepository(person, startDate, steps);
            }
        });
        Log.d("SWELL", String.format("Downloading %s\'s fitness data from %s",
                person.getPerson().getName(), startDate.getTime().toString()));
    }

    /* UPLOADING METHODS */
    private void doUploadToRepository(final StorywellPerson person,
                                      Calendar startDate, List<Integer> steps) {
        this.status.postValue(SyncStatus.UPLOADING);
        int minutesElapsed = steps.size() - SAFE_MINUTES;
        person.setLastSyncTime(getApplication(),
                WellnessDate.getCalendarAfterNMinutes(startDate, minutesElapsed));
        final Date date = startDate.getTime();
        this.fitnessRepository.insertIntradaySteps(person.getPerson(), startDate.getTime(), steps,
                new onDataUploadListener() {
            @Override
            public void onSuccess() {
                doUpdateDailyFitness(person.getPerson(), date);
            }

            @Override
            public void onFailed() {
                Log.e("SWELL", String.format("Error uploading %s fitness data",
                        currentPerson.getPerson().getName()));
            }
        });
    }

    private void doUpdateDailyFitness(Person person, Date startDate) {
        this.fitnessRepository.updateDailyFitness(person, startDate, new onDataUploadListener(){

            @Override
            public void onSuccess() {
                doCompleteOneBtDevice();
            }

            @Override
            public void onFailed() {
                Log.e("SWELL", String.format("Error updating %s daily fitness data",
                        currentPerson.getPerson().getName()));
            }
        });
    }

    /* COMPLETION METHODS */
    private void doCompleteOneBtDevice() {
        this.status.postValue(SyncStatus.IN_PROGRESS);
    }

    /* STORYWELL HELPER */
    private List<StorywellPerson> getStorywellMembers(Group group) {
        List<StorywellPerson> storywellPeople = new ArrayList<>();
        for (Person person : group.getMembers()) {
            storywellPeople.add(StorywellPerson.newInstance(person, this.getApplication()));
        }
        return storywellPeople;
    }
}