package edu.neu.ccs.wellness.storytelling.storyview;

import android.content.Context;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.geostory.GeoStoryMeta;
import edu.neu.ccs.wellness.geostory.GeoStoryResponseManager;
import edu.neu.ccs.wellness.reflection.ReflectionManager;
import edu.neu.ccs.wellness.server.RestServer;
import edu.neu.ccs.wellness.story.StoryChallenge;
import edu.neu.ccs.wellness.story.StoryChapterManager;
import edu.neu.ccs.wellness.story.StoryContentManager;
import edu.neu.ccs.wellness.story.StoryCover;
import edu.neu.ccs.wellness.story.interfaces.StoryContent;
import edu.neu.ccs.wellness.story.interfaces.StoryContentState;
import edu.neu.ccs.wellness.story.interfaces.StoryInterface;
import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSetting;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSettingRepository;
import edu.neu.ccs.wellness.storytelling.utils.NearbyPlacesManagerInterface;
import edu.neu.ccs.wellness.storytelling.utils.PlaceItem;
import edu.neu.ccs.wellness.storytelling.utils.UserLogging;

/**
 * Created by hermansaksono on 1/23/19.
 */

public class StoryViewPresenter implements
        ReflectionFragment.ReflectionFragmentListener,
        StatementFragment.StatementFragmentListener,
        GeoStorySharingFragment.GeoStoryFragmentListener,
        NearbyPlacesManagerInterface {
    private StoryInterface story;
    private Storywell storywell;
    private ReflectionManager reflectionManager;
    private StoryContentManager storyContentManager;
    private GeoStoryResponseManager geoStoryResponseManager;
    private StoryChapterManager storyChapterManager;

    private int currentPagePosition = 0;

    public StoryViewPresenter(final FragmentActivity activity, StoryInterface story) {
        this.storywell = new Storywell(activity);
        this.story = story;
        this.storyChapterManager = new StoryChapterManager(activity.getApplicationContext());
        this.reflectionManager = new ReflectionManager(
                this.storywell.getGroup().getName(),
                this.story.getId(),
                this.storywell.getReflectionIteration(),
                this.storywell.getReflectionIterationMinEpoch(),
                activity.getApplicationContext());
        this.storyContentManager = new StoryContentManager(
                this.story.getId(),
                this.storywell.getGroup().getName());
        this.geoStoryResponseManager = new GeoStoryResponseManager(
                this.storywell.getGroup().getName(),
                this.story.getId(),
                activity.getApplicationContext());
    }

    /* STORY DOWNLOAD METHODS */
    public RestServer.ResponseType asyncLoadStory(Context context) {
        return story.tryLoadStoryDef(context, storywell.getServer(), storywell.getGroup());
    }

    /* STORY STATE SAVINGS */
    public void doSaveStoryState(Context context) {
        /*
        SharedPreferences sharedPreferences = WellnessIO.getSharedPref(context);
        SharedPreferences.Editor putPositionInPref = sharedPreferences.edit();
        putPositionInPref.putInt("lastPagePositionSharedPref", this.currentPagePosition).apply();
        this.story.saveState(context, storywell.getGroup());
        */
        SynchronizedSetting setting = storywell.getSynchronizedSetting();
        setting.getStoryListInfo().getCurrentStoryPageId().put(story.getId(), currentPagePosition);
        SynchronizedSettingRepository.saveLocalAndRemoteInstance(setting, context);
    }

    public void doRefreshStoryState(Context context) {
        /*
        SharedPreferences sharedPreferences = WellnessIO.getSharedPref(context);
        this.currentPagePosition = sharedPreferences.getInt("lastPagePositionSharedPref", 0);
        */
        SynchronizedSetting setting = storywell.getSynchronizedSetting();
        if (setting.getStoryListInfo().getCurrentStoryPageId().containsKey(story.getId())) {
            this.currentPagePosition = setting.getStoryListInfo().getCurrentStoryPageId()
                    .get(story.getId());
        } else {
            this.currentPagePosition = 0;
        }
    }

    /* GEOSTORY METHODS */
    @Override
    public boolean isGeoStoryExists(String promptId) {
        return this.geoStoryResponseManager.isReflectionResponded(promptId);
    }

    @Override
    public GeoStory getSavedGeoStory(String promptId) {
        return this.geoStoryResponseManager.getSavedGeoStory(promptId);
    }

    @Override
    public void doStartGeoStoryRecording(String promptParentId, String promptId) {
        if (geoStoryResponseManager.getIsPlayingStatus()) {
            this.geoStoryResponseManager.stopPlayback();
        }

        if (geoStoryResponseManager.getIsRecordingStatus() == false) {
            UserLogging.logGeoStoryRecordButtonPressed(this.story.getId(), promptId);
            this.geoStoryResponseManager.startRecording(
                    promptId,
                    "",
                    "",
                    new MediaRecorder());
        }
    }

    @Override
    public void doStopGeoStoryRecording() {
        if (geoStoryResponseManager.getIsRecordingStatus()) {
            this.geoStoryResponseManager.stopRecording();
        }
    }

    @Override
    public void doStartGeoStoryPlay(String promptId, MediaPlayer.OnCompletionListener completionListener) {
        String reflectionUrl = this.geoStoryResponseManager.getRecordingURL(promptId);
        if (this.geoStoryResponseManager.getIsPlayingStatus()) {
            // Don't do anything
        } else if (reflectionUrl != null) {
            UserLogging.logGeoStoryPlayButtonPressed(this.story.getId(), promptId);
            this.geoStoryResponseManager.startPlayback(
                    reflectionUrl, new MediaPlayer(), completionListener);
        }
    }

    @Override
    public void doStopGeoStoryPlay() {
        this.geoStoryResponseManager.stopPlayback();
    }

    @Override
    public boolean doShareGeoStory(Location location, GeoStoryMeta geoStoryMeta) {
        this.geoStoryResponseManager.setLocation(location);
        this.geoStoryResponseManager.setGeoStoryMeta(geoStoryMeta);
        if (this.geoStoryResponseManager.isUploadQueued()) {
            new AsyncUploadGeoStory(geoStoryResponseManager).execute();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public FusedLocationProviderClient getLocationProvider() {
        // DON'T DO ANYTHING. USE ACTIVITY INSTEAD
        return null;
    }

    @Override
    public boolean isMoodLogResponded(int contentId) {
        String contentStatus = this.storyContentManager.getStatus(contentId);

        if (StoryContentState.RESPONDED.equals(contentStatus)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setMoodLogResponded(int contentId) {
        this.storyContentManager.setStatus(contentId, StoryContentState.RESPONDED);
    }

    /* FIELDS & METHODS FOR NEARBY PLACES */
    private List<PlaceItem> placeItemList;

    @Override
    public void setPlaceItemList(List<PlaceItem> placeItemList) {
        this.placeItemList = placeItemList;
    }

    @Override
    public List<PlaceItem> getPlaceItemList() {
        return this.placeItemList;
    }

    public static class AsyncUploadGeoStory extends AsyncTask<Void, Void, GeoStory> {
        GeoStoryResponseManager responseManager;

        AsyncUploadGeoStory(GeoStoryResponseManager responseManager) {
            this.responseManager = responseManager;
        }

        @Override
        protected GeoStory doInBackground(Void... voids) {
            responseManager.uploadReflectionAudioToFirebase();
            return responseManager.getCurrentGeoStory();
        }

        @Override
        protected void onPostExecute(GeoStory geoStory) {
            super.onPostExecute(geoStory);
            if (geoStory != null) {
                String promptParentId = geoStory.getMeta().getPromptParentId();
                String promptId = geoStory.getMeta().getPromptId();
                UserLogging.logGeoStorySubmitted(promptParentId, promptId, geoStory.getStoryId());
            }
        }
    }

    /* REFLECTION DONWLOAD AND UPLOAD METHODS */
    public void loadReflectionUrls(ValueEventListener listener) {
        this.reflectionManager.getReflectionUrlsFromFirebase(
                this.storywell.getReflectionIterationMinEpoch(), listener);
    }

    public boolean uploadReflectionAudio() {
        if (this.reflectionManager.isUploadQueued()) {
            new AsyncUploadAudio().execute();
            UserLogging.logReflectionResponded(this.story.getId(), currentPagePosition);
            return true;
        } else {
            return false;
        }
    }

    public class AsyncUploadAudio extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            reflectionManager.uploadReflectionAudioToFirebase();
            return null;
        }
    }

    /* REFLECTION METHODS */
    @Override
    public boolean isReflectionExists(int contentId) {
        return this.reflectionManager.isReflectionResponded(String.valueOf(contentId));
    }

    @Override
    public void doStartRecording(int contentId, String contentGroupId, String contentGroupName) {
        if (reflectionManager.getIsPlayingStatus() == true) {
            this.reflectionManager.stopPlayback();
        }

        if (reflectionManager.getIsRecordingStatus() == false) {
            UserLogging.logReflectionRecordButtonPressed(this.story.getId(), contentId);
            this.reflectionManager.startRecording(
                    String.valueOf(contentId),
                    contentGroupId,
                    contentGroupName,
                    new MediaRecorder());
        }
    }

    @Override
    public void doStopRecording() {
        if (reflectionManager.getIsRecordingStatus() == true) {
            this.reflectionManager.stopRecording();
        }
    }

    @Override
    public void doStartPlay(int contentId, MediaPlayer.OnCompletionListener completionListener) {
        String reflectionUrl = this.reflectionManager.getRecordingURL(String.valueOf(contentId));
        if (this.reflectionManager.getIsPlayingStatus()) {
            // Don't do anything
        } else if (reflectionUrl != null) {
            UserLogging.logReflectioPlayButtonPressed(this.story.getId(), currentPagePosition);
            this.reflectionManager.startPlayback(
                    reflectionUrl, new MediaPlayer(), completionListener);
        }
    }

    @Override
    public void doStopPlay() {
        this.reflectionManager.stopPlayback();
    }

    /* PAGE NAVIGATION METHODS */
    public int getCurrentPagePosition() {
        return this.currentPagePosition;
    }

    public boolean tryGoToThisPage(
            int position, ViewPager viewPager, StoryInterface story, Context context) {
        int allowedPosition = getAllowedPageToGo(position);
        viewPager.setCurrentItem(allowedPosition);
        this.currentPagePosition = allowedPosition;

        if (allowedPosition == position) {
            return true;
        } else {
            this.doExplainWhyProceedingIsNotAllowed(allowedPosition, context);
            return false;
        }
    }

    private int getAllowedPageToGo(int goToPosition) {
        int preceedingPosition = goToPosition - 1;
        if (preceedingPosition < 0) {
            return goToPosition;
        } else {
            StoryContent precContent = this.story.getContentByIndex(preceedingPosition);
            if (canProceedToNextContent(precContent)) {
                return goToPosition;
            } else {
                return preceedingPosition;
            }
        }
    }
    private boolean canProceedToNextContent(StoryContent currentContent) {
        switch (currentContent.getType()) {
            case COVER:
                return canProceedFromThisCover(currentContent);
            case REFLECTION:
                return canProceedFromThisReflection(currentContent);
            case CHALLENGE:
                return canProceedFromThisChallenge(currentContent);
            case GEOSTORY_SHARING:
                return canProceedFromThisGeoStorySharing(currentContent);
            default:
                return true;
        }
    }

    private boolean canProceedFromThisCover(StoryContent thisCover) {
        StoryCover storyCover = (StoryCover) thisCover;
        if (storyCover.isLocked()) {
            return this.storyChapterManager.isThisChapterUnlocked(storyCover.getStoryPageId());
        } else {
            return true;
        }
    }

    private boolean canProceedFromThisReflection(StoryContent thisReflection) {
        return this.isReflectionExists(thisReflection.getId());
    }

    private boolean canProceedFromThisChallenge(StoryContent thisChallenge) {
        StoryChallenge storyChallenge = (StoryChallenge) thisChallenge;
        return this.storyChapterManager.isThisChapterUnlocked(storyChallenge.getStoryPageId());
    }

    private boolean canProceedFromThisGeoStorySharing(StoryContent thisGeoStory) {
        String promptId = String.valueOf(thisGeoStory.getId());
        return this.geoStoryResponseManager.isReflectionResponded(promptId);
    }

    public void doExplainWhyProceedingIsNotAllowed(int allowedPosition, Context context) {
        StoryContent content = this.story.getContentByIndex(allowedPosition);
        switch (content.getType()) {
            case COVER:
                doTellUserCoverIsLocked(context);
                break;
            case REFLECTION:
                // Do nothing for now
            case CHALLENGE:
                // Do nothing for now
            default:
                // Do nothing for now
        }
    }

    /* CHALLENGE RELATED METHODS */
    public void setCurrentStoryChapterAsLocked(Context context) {
        StoryChallenge storyChallenge =
                (StoryChallenge) story.getContentByIndex(currentPagePosition);
        this.storyChapterManager.setThisStoryPageForChallenge(
                story, storyChallenge.getStoryPageId(), context);
    }


    /* TOASTS RELATED METHODS */
    private void doTellUserCoverIsLocked(Context context) {
        Toast.makeText(context, R.string.story_view_cover_locked, Toast.LENGTH_SHORT).show();
    }

    /* ANIMATION METHODS */
    public static void animateEnvelopeBouncing(final View envelopeView) {
        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                float translationY = (float) Math.sin(interpolatedTime * 2 * Math.PI) * 25;
                envelopeView.setTranslationY(translationY);
            }
        };

        anim.setInterpolator(new LinearInterpolator());
        anim.setDuration(1500);
        anim.setRepeatCount(15);
        anim.setFillAfter(true);
        envelopeView.startAnimation(anim);
    }
}
