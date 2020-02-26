package edu.neu.ccs.wellness.story.interfaces;

public interface StorytellingContentManager {

    String getStoryId();

    void setStatus(int contentId, String status);

    String getStatus(int contentId);
}
