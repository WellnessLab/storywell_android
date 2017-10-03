package edu.neu.ccs.wellness.storytelling.models;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import edu.neu.ccs.wellness.storytelling.interfaces.RestServer;
import edu.neu.ccs.wellness.storytelling.interfaces.StoryInterface;
import edu.neu.ccs.wellness.storytelling.interfaces.StorytellingManager;
import edu.neu.ccs.wellness.storytelling.interfaces.StorytellingException;

/**
 * Created by hermansaksono on 6/13/17.
 */

public class StoryManager implements StorytellingManager {
    public static final String STORY_ALL = "group/stories/all";
    public static final String PREFS_NAME = "WELLNESS_STORYTELLING";

    public static final String FILENAME_STORY_LIST = "story_list";

    private static final String EXC_STORIES_UNINITIALIZED = "Story list has not been initialized";
    private static final String EXC_STORY_EXIST_FALSE = "Story does not exist";

    private RestServer server;
    private ArrayList<StoryInterface> storyList;
    private String lastRefreshDateTime;
    private StoryInterface currentStory;
    private String dateTime;

    // PRIVATE CONSTRUCTORS
    private StoryManager(RestServer server, ArrayList<StoryInterface> storyList) {
        this.server = server;
        this.storyList = storyList;
        this.lastRefreshDateTime = null;
        //this.currentStory = getCurrentStoryFromList(storyList);
    }

    // STATIC FACTORY METHODS
    public static StoryManager create(RestServer server) {
        // TODO If Json file is in the local storage, then load, otherwise set null
        return new StoryManager(server, null);
    }

    // PUBLIC METHODS
    @Override
    public boolean isStoryListSet() {
        return (this.storyList != null);
    }

    @Override
    public ArrayList<StoryInterface> getStoryList() {
        return this.storyList;
    }

    @Override
    public String getLastStoryListRefreshDateTime() {
        return this.lastRefreshDateTime;
    }

    @Override
    public int getCurrentStoryId() {
        return this.currentStory.getId();
    }

    @Override
    public StoryInterface getStoryById(int storyId) throws StorytellingException {
        for (StoryInterface story:this.storyList) {
            if (story.getId() == storyId) { return story; }
        }
        throw new StorytellingException(EXC_STORY_EXIST_FALSE);
    }

    @Override
    public StoryInterface getCurrentStory() { return this.currentStory; }

    /**
     * Checks if internet connection is available
     * @param context Android application Context
     * @return true if internet connection is available, otherwise return false
     */
    public boolean canAccessServer (Context context) { return this.server.isOnline(context); }

    /***
     * If the storyListFile doesn't exist in the internal storage, then do an HTTP GET request
     * from the server and save the response to the internal storage.
     * Then, load the storyListFile from internal storage, then convert it to a JSON object.
     * Finally generate a list of uninitialized Story objects from the JSON object and assign it to
     * the instance's storyList.
     * @param context The Android context to assist saving files to internal storage.
     */
    public void loadStoryList(Context context) {
        try {
            String jsonString = this.server.loadGetRequest(context, FILENAME_STORY_LIST, STORY_ALL);
            JSONObject jsonObject = new JSONObject(jsonString);
            this.storyList = this.getStoryListFromJSONArray(jsonObject.getJSONArray("stories"));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /***
     * If storyList has been initialized, then iterate on the stories to load each Story
     * definitions.
     * @param context The Android context to assist saving files to internal storage.
     */
    public void downloadStoryDef (Context context, int position) throws StorytellingException {
        if (isStoryListSet()) {
            Story story = (Story) this.storyList.get(position);
            story.downloadStoryDef(context, this.server);
        }
        else {
            throw new StorytellingException(EXC_STORIES_UNINITIALIZED);
        }
    }

    // HELPER FUNCTIONS
    private ArrayList<StoryInterface> getStoryListFromJSONArray(JSONArray jsonList)
            throws JSONException {
        ArrayList<StoryInterface> storyList = new ArrayList<StoryInterface>();
        for(int i = 0; i < jsonList.length(); i++) {
            JSONObject jsonStory = jsonList.getJSONObject(i);
            storyList.add(Story.create(jsonStory));
        }
        return storyList;
    }

    private static StoryInterface getCurrentStoryFromList (ArrayList<StoryInterface> storyList) {
        for (StoryInterface story:storyList) {
            if (story.isCurrent()) {
                return story;
            }
        }
        return null;
    }
}