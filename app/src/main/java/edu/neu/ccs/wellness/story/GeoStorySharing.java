package edu.neu.ccs.wellness.story;

import android.content.Context;

import edu.neu.ccs.wellness.server.RestServer;
import edu.neu.ccs.wellness.story.interfaces.StoryContent;
import edu.neu.ccs.wellness.story.interfaces.StoryInterface;
import edu.neu.ccs.wellness.story.interfaces.StorytellingException;

/**
 * Created by hermansaksono on 6/14/17.
 */

public class GeoStorySharing implements StoryContent {

    public final static String KEY_PROMPT_PARENT_ID = "KEY_PROMPT_PARENT_ID";

    private StoryPage page;
    private String promptParentId;

    // CONSTRUCTORS
    public GeoStorySharing(int pageId, StoryInterface story, String imgUrl,
                           String text, String subText, boolean isCurrentPage) {
        this.page = new StoryPage(pageId, story, imgUrl,
                text, subText, isCurrentPage, false);
        this.promptParentId = story.getId();
    }

    // PUBLIC METHODS

    @Override
    public int getId() {
        return this.page.getId();
    }

    @Override
    public void downloadFiles(Context context, RestServer server)
            throws StorytellingException {
        this.page.downloadFiles(context, server);
    }

    @Override
    public ContentType getType() {
        return ContentType.GEOSTORY_SHARING;
    }

    @Override
    public String getImageURL() { return this.page.getImageURL(); }

    @Override
    public String getText() {
        return this.page.getText();
    }

    @Override
    public String getSubtext() {
        return this.page.getSubtext();
    }

    @Override
    public boolean isCurrent() {
        return this.page.isCurrent();
    }

    @Override
    public void setIsCurrent(boolean isCurrent) {
        this.page.setIsCurrent(isCurrent);
    }

    @Override
    public void respond() { }

    @Override
    public boolean isLocked() {
        return false;
    }

    public String getPromptParentId() {
        return promptParentId;
    }
}
