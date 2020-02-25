package edu.neu.ccs.wellness.story;

import android.content.Context;

import edu.neu.ccs.wellness.server.RestServer;
import edu.neu.ccs.wellness.story.interfaces.StoryContent;
import edu.neu.ccs.wellness.story.interfaces.StoryInterface;
import edu.neu.ccs.wellness.story.interfaces.StorytellingException;

/**
 * Created by hermansaksono on 6/14/17.
 */

public class StoryStatement implements StoryContent {
    public static final String KEY_IS_INVITE_LOG_MOOD = "isInviteMoodLog";
    public static final boolean DEFAULT_IS_INVITE_LOG_MOOD = false;

    private StoryPage page;
    private boolean isShowMoodLog;

    // CONSTRUCTORS

    public StoryStatement(int pageId, StoryInterface story,
                          String imgUrl, String text, String subText,
                          boolean isCurrentPage) {
        this.page = new StoryPage(pageId, story, imgUrl, text, subText, isCurrentPage, false);
        this.isShowMoodLog = false;
    }

    public StoryStatement(int pageId, StoryInterface story,
                          String imgUrl, String text, String subText,
                          boolean isCurrentPage, boolean isInviteMoodLog) {
        this.page = new StoryPage(pageId, story, imgUrl, text, subText, isCurrentPage, false);
        this.isShowMoodLog = isInviteMoodLog;
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
        return ContentType.STATEMENT;
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

    public boolean isInviteMoodLog() { return this.isShowMoodLog; }
}
