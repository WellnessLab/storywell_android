package edu.neu.ccs.wellness.storytelling.utils;

import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.geostory.GeoStoryMeta;
import edu.neu.ccs.wellness.logging.Param;
import edu.neu.ccs.wellness.logging.WellnessUserLogging;
import edu.neu.ccs.wellness.story.Story;
import edu.neu.ccs.wellness.story.interfaces.StoryInterface;

/**
 * Created by hermansaksono on 3/12/19.
 */

public class UserLogging {

    public static void logStartup() {
        getLogger().logEvent("APP_STARTUP", null);
    }

    public static void logStoryView(StoryInterface story) {
        Bundle bundle = new Bundle();
        bundle.putString("STORY_ID", story.getId());
        // TODO need to log story page
        //bundle.putInt("STORY_ID", story.getState().getCurrentPage());
        getLogger().logEvent("READ_STORY", bundle);
    }

    public static void logStoryUnlocked(String storyId, String pageId) {
        Bundle bundle = new Bundle();
        bundle.putString("STORY_ID", storyId);
        bundle.putString("STORY_PAGE_ID", pageId);
        getLogger().logEvent("STORY_UNLOCKED", bundle);
    }

    public static void logButtonPlayPressed() {
        Bundle bundle = new Bundle();
        bundle.putString(Param.BUTTON_NAME, "PLAY_ANIMATION");
        getLogger().logEvent("PLAY_BUTTON_CLICK", bundle);
    }

    public static void logProgressAnimation(
            float adultProgress, float childProgress, float overallProgress) {
        Bundle bundle = new Bundle();
        bundle.putFloat("ADULT_PROGRESS", adultProgress);
        bundle.putFloat("CHILD_PROGRESS", childProgress);
        bundle.putFloat("OVERALL_PROGRESS", overallProgress);
        getLogger().logEvent("PLAY_PROGRESS_ANIMATION", bundle);
    }

    public static void logStartBleSync() {
        getLogger().logEvent("SYNC_START", null);
    }

    public static void logStopBleSync(boolean isSuccesful) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("IS_SUCCESSFUL", isSuccesful);
        getLogger().logEvent("SYNC_ENDED", bundle);
    }

    public static void logBleFailed() {
        Bundle bundle = new Bundle();
        getLogger().logEvent("SYNC_FAILED", bundle);
    }

    public static void logViewTreasure(String treasureParentId, int treasureContentId) {
        Bundle bundle = new Bundle();
        bundle.putString("STORY_ID", treasureParentId);
        bundle.putInt("REFLECTION_START_CONTENT_ID", treasureContentId);
        getLogger().logEvent("VIEW_TREASURE", bundle);
    }

    public static void logResolutionState(String rouletteState) {
        Bundle bundle = new Bundle();
        bundle.putString("ROULETTE_STATE", rouletteState);
        getLogger().logEvent("RESOLUTION_CHOSEN", bundle);
    }

    public static void logResolutionIdeaChosen(int ideaGroup, int ideaId) {
        Bundle bundle = new Bundle();
        bundle.putInt("IDEA_GROUP", ideaGroup);
        bundle.putInt("IDEA_ID", ideaId);
        getLogger().logEvent("RESOLUTION_IDEA_CHOSEN", bundle);
    }

    public static void logChallengeViewed() {
        getLogger().logEvent("CHALLENGE_VIEWED", null);
    }

    public static void logChallengePicked(String challengeJson) {
        Bundle bundle = new Bundle();
        bundle.putString("CHALLENGE_JSON", challengeJson);
        getLogger().logEvent("CHALLENGE_PICKED", bundle);
    }

    public static void logReflectionRecordButtonPressed(String storyId, int pageId) {
        Bundle bundle = new Bundle();
        bundle.putString("STORY_ID", storyId);
        bundle.putInt("PAGE_ID", pageId);
        getLogger().logEvent("REFLECTION_ANSWERING_START", bundle);
    }

    public static void logReflectioPlayButtonPressed(String storyId, int pageId) {
        Bundle bundle = new Bundle();
        bundle.putString("STORY_ID", storyId);
        bundle.putInt("PAGE_ID", pageId);
        getLogger().logEvent("REFLECTION_PLAYBACK_START", bundle);
    }

    public static void logReflectionResponded(String storyId, int pageId) {
        Bundle bundle = new Bundle();
        bundle.putString("STORY_ID", storyId);
        bundle.putInt("PAGE_ID", pageId);
        bundle.putString("TRANSCRIPT", " ");
        getLogger().logEvent("REFLECTION_RESPONDED", bundle);
    }

    public static void logReflectioDeleted(String storyId, int pageId) {
        Bundle bundle = new Bundle();
        bundle.putString("STORY_ID", storyId);
        bundle.putInt("PAGE_ID", pageId);
        getLogger().logEvent("REFLECTION_DELETE_ATTEMPTED", bundle);
    }
    
    public static void logGeoStoryRecordButtonPressed(String promptParentId, String promptId) {
        Bundle bundle = new Bundle();
        bundle.putString("PROMPT_PARENT_ID", promptParentId);
        bundle.putString("PROMPT_ID", promptId);
        getLogger().logEvent("GEOSTORY_RECORDING_START", bundle);
    }

    public static void logGeoStoryPlayButtonPressed(String promptParentId, String promptId) {
        Bundle bundle = new Bundle();
        bundle.putString("PROMPT_PARENT_ID", promptParentId);
        bundle.putString("PROMPT_ID", promptId);
        getLogger().logEvent("GEOSTORY_PLAYBACK_START", bundle);
    }

    public static void logGeoStoryMetaEdited(String promptParentId, String promptId, GeoStoryMeta meta) {
        Bundle bundle = new Bundle();
        bundle.putString("PROMPT_PARENT_ID", promptParentId);
        bundle.putString("PROMPT_ID", promptId);
        bundle.putString("GEOSTORY_META", new Gson().toJson(meta));
        bundle.putString("TRANSCRIPT", " ");
        getLogger().logEvent("GEOSTORY_META_EDITED", bundle);
    }

    public static void logGeoStorySubmitted(String promptParentId, String promptId, String id) {
        Bundle bundle = new Bundle();
        bundle.putString("PROMPT_PARENT_ID", promptParentId);
        bundle.putString("PROMPT_ID", promptId);
        bundle.putString("GEOSTORY_ID", id);
        getLogger().logEvent("GEOSTORY_SUBMITTED", bundle);
    }

    public static void logAdultEmotion(String listOfEmotionsJson) {
        Bundle bundle = new Bundle();
        bundle.putString("role", "ADULT");
        bundle.putString("list_of_emotions", listOfEmotionsJson);
        getLogger().logEvent("EMOTION_LOGGED", bundle);
    }

    public static void logChildEmotion(String listOfEmotionsJson) {
        Bundle bundle = new Bundle();
        bundle.putString("role", "CHILD");
        bundle.putString("list_of_emotions", listOfEmotionsJson);
        getLogger().logEvent("EMOTION_LOGGED", bundle);
    }

    private static WellnessUserLogging getLogger() {
        return new WellnessUserLogging(getUid());
    }

    private static String getUid() {
        return FirebaseAuth.getInstance().getUid();
    }
}
