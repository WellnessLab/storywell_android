package edu.neu.ccs.wellness.storytelling.interfaces;

import java.util.List;

/**
 * Created by hermansaksono on 6/13/17.
 */

public interface StorytellingManager {

    public boolean isStoryListSet();

    public List<StoryInterface> getStoryList();

    public String getLastStoryListRefreshDateTime();

    public int getCurrentStoryId();

    public StoryInterface getStoryById(int storyId) throws StorytellingException;

    public StoryInterface getCurrentStory();
}