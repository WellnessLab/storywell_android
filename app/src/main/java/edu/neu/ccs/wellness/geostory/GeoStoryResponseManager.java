package edu.neu.ccs.wellness.geostory;

import android.content.Context;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.view.SurfaceHolder;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import edu.neu.ccs.wellness.reflection.ResponseManager;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSetting;

/**
 * Created by hermansaksono on 3/5/18.
 */

public class GeoStoryResponseManager extends ResponseManager {
    private boolean isPlaying = false;
    private boolean isRecording = false;
    private final String groupName;
    private GeoStory currentGeoStory;
    private GeoStoryMeta currentGeoStoryMeta;
    private String currentRecordingAudioFile;
    private boolean isUploadQueueNotEmpty = false;
    private MediaPlayer mediaPlayer;
    private MediaRecorder mediaRecorder;
    private FirebaseGeoStoryRepository responseRepository;
    private String cachePath;
    private Location location = new Location("dummyProvider");
    private String promptParentId;

    private SynchronizedSetting synchronizedSetting;


    /* CONSTRUCTOR */
    public GeoStoryResponseManager(String groupName, String storyId, Context context) {
        Storywell storywell = new Storywell(context);
        this.synchronizedSetting = storywell.getSynchronizedSetting();
        this.groupName = groupName;
        this.promptParentId = storyId;
        this.responseRepository = new FirebaseGeoStoryRepository(groupName, storyId);
        this.cachePath = context.getCacheDir().getAbsolutePath() + "/";
    }

    /* GENERAL METHODS */
    @Override
    public boolean getIsPlayingStatus() {
        return this.isPlaying;
    }

    public boolean getIsRecordingStatus() {
        return this.isRecording;
    }

    private void setIsPlayingState(boolean status) {
        this.isPlaying = status;
    }

    private void setIsRecordingState(boolean status) {
        this.isRecording = status;
    }

    public GeoStory getCurrentGeoStory() {
        return this.currentGeoStory;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setGeoStoryMeta(GeoStoryMeta geoStoryMeta) {
        this.currentGeoStoryMeta = geoStoryMeta;
    }

    @Override
    public boolean isReflectionResponded(String promptId) {
        return this.responseRepository.isReflectionResponded(promptId);
    }

    @Override
    public String getRecordingURL(String promptId) {
        return this.responseRepository.getRecordingURL(promptId);
    }

    /* AUDIO PLAYBACK METHODS */
    @Override
    public void startPlayback(String audioPath, MediaPlayer mediaPlayer,
                              final OnCompletionListener completionListener) {
        /*
        if (! new File(audioPath).exists()) {
            completionListener.onCompletion(mediaPlayer);
            return;
        }
        */

        this.setIsPlayingState(true);
        this.mediaPlayer = mediaPlayer;
        try {
            this.mediaPlayer.setDataSource(audioPath);
            this.mediaPlayer.prepare();
            this.mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            completionListener.onCompletion(mediaPlayer);
            this.setIsPlayingState(false);
        }

        this.mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlayback();
                completionListener.onCompletion(mediaPlayer);
            }
        });
    }

    @Override
    public void stopPlayback() {
        if (this.mediaPlayer != null) {
            if (this.mediaPlayer.isPlaying()) {
                this.mediaPlayer.stop();
            }
            this.mediaPlayer.release();
            this.mediaPlayer = null;
            this.setIsPlayingState(false);
        }
    }

    /* AUDIO RECORDING METHODS */
    @Override
    public void startRecording(String promptId,
                               String reflectionGroupId, String reflectionGroupName,
                               MediaRecorder mediaRecorder) {
        if (this.isPlaying) {
            this.stopPlayback();
        }
        if (!this.isRecording) {
            this.setIsRecordingState(true);

            // Set up the GeoStoryMeta
            this.currentGeoStoryMeta = new GeoStoryMeta();
            this.currentGeoStoryMeta.setPromptParentId(this.promptParentId);
            this.currentGeoStoryMeta.setPromptId(promptId);
            this.currentGeoStoryMeta.setUserNickname(synchronizedSetting
                    .getFamilyInfo().getCaregiverNickname());
            this.currentGeoStoryMeta.setBio(synchronizedSetting
                    .getFamilyInfo().getCaregiverBio());

            // Set up the GeoStory
            this.currentGeoStory = new GeoStory();
            this.currentGeoStory.setUsername(this.groupName);
            this.currentGeoStory.setMeta(this.currentGeoStoryMeta);

            this.currentRecordingAudioFile = getOutputFilePath(cachePath, currentGeoStory);
            this.isUploadQueueNotEmpty = true;

            this.mediaRecorder = mediaRecorder;
            this.mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            this.mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            this.mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            this.mediaRecorder.setOutputFile(this.currentRecordingAudioFile);

            try {
                this.mediaRecorder.prepare();
                this.mediaRecorder.start();
            } catch (IOException e) {
                this.setIsRecordingState(false);
                if (this.mediaRecorder != null) {
                    this.mediaRecorder.stop();
                    this.mediaRecorder.reset();
                    this.mediaRecorder.release(); // TODO may cause bugs
                }
            }
        }
    }

    private String getOutputFilePath(String cachePath, GeoStory geoStory) {
        StringBuilder sb = new StringBuilder();
        sb.append(cachePath);
        sb.append(geoStory.getFilename());
        return sb.toString();
    }

    @Override
    public void stopRecording() {
        if (this.mediaRecorder != null && this.isRecording) {
            this.currentGeoStory.setStoryUri(this.currentRecordingAudioFile);
            this.responseRepository.putRecordingURL(currentGeoStory);
            this.mediaRecorder.stop();
            this.mediaRecorder.reset();
            this.mediaRecorder.release();
            this.mediaRecorder = null;
            this.setIsRecordingState(false);
        }
    }

    /* FIREBASE STORAGE PUBLIC METHODS*/
    @Override
    public boolean isUploadQueued() {
        return this.isUploadQueueNotEmpty;
    }

    @Override
    public void getReflectionUrlsFromFirebase(
            long reflectionMinEpoch, ValueEventListener listener) {
        this.responseRepository.getUserGeoStoriesFromFirebase(groupName, promptParentId);
    }

    @Override
    public void uploadReflectionAudioToFirebase() {
        this.currentGeoStory.setLatitude(location.getLatitude());
        this.currentGeoStory.setLongitude(location.getLongitude());
        this.currentGeoStory.setLastUpdateTimestamp(
                Calendar.getInstance(Locale.US).getTimeInMillis());
        this.currentGeoStory.setMeta(this.currentGeoStoryMeta);

        this.responseRepository.uploadGeoStoryFileToFirebase(
                currentGeoStory, currentRecordingAudioFile,
                new OnSuccessListener<UploadTask.TaskSnapshot>(){
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        isUploadQueueNotEmpty = false;
                    }
                });
    }

    /* VIDEO RECORDING (NOT IMPLEMENTED) */
    /**
     *
     * Ref: https://stackoverflow.com/questions/1817742/how-can-i-record-a-video-in-my-android-app
     * @param contentId
     */
    @Override
    public void startVideoRecording(String contentId, MediaRecorder mediaRecorder) {
        // NOT IMPLEMENTED
    }

    @Override
    public void stopVideoRecording() {
        // NOT IMPLEMENTED
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // NOT IMPLEMENTED
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        // NOT IMPLEMENTED

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // NOT IMPLEMENTED
    }

}
