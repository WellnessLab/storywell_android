package edu.neu.ccs.wellness.storytelling.storyview;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import edu.neu.ccs.wellness.storytelling.R;

/**
 * A Fragment to show a simple view of one artwork and one text of the Story.
 */
public class StoryCoverFragment extends Fragment {
    private final DisplayImageOptions options = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.img_placeholder)
            .showImageForEmptyUri(R.drawable.img_failure)
            .showImageOnFail(R.drawable.img_failure)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public StoryCoverFragment() {
    }

    /**
     * The system calls onCreateView when it's time for the fragment to draw its user interface
     * for the first time.
     * So the view gets inflated which is considered one of the most heavy tasks in Android
     * Do all essential initializations in onCreate of Fragments above
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_story_cover, container, false);
        ImageView imageView = view.findViewById(R.id.storyImage);
        ImageLoader imageLoader = ImageLoader.getInstance();


        // If the values don't reach due to some error
        // Do it in a try-catch block so that app doesn't crash
        try {
            setTitleText(view, getArguments().getString("KEY_TEXT"));
            setAuthorText(view, getArguments().getString("KEY_SUBTEXT"));
            imageLoader.displayImage(getArguments().getString("KEY_IMG_URL"), imageView, options);
            setLockedInfo(view, getArguments().getBoolean("KEY_IS_LOCKED", false));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return view;
    }

    /***
     * Set View to show the Story's content
     * @param view The View in which the content will be displayed
     * @param text The Storybook's title
     */
    private void setTitleText(View view, String text) {
        TextView tv = view.findViewById(R.id.storyText);
        tv.setText(text);
    }
    private void setAuthorText(View view, String text) {
        if (text != null && text.length() != 0) {
            TextView tv = view.findViewById(R.id.storyAuthor);
            tv.setText(text);
            tv.setVisibility(View.VISIBLE);
        }
    }

    private void setLockedInfo(View view, boolean key_is_locked) {
        if (key_is_locked) {
            view.findViewById(R.id.storyImage_locked).setVisibility(View.VISIBLE);
            view.findViewById(R.id.layout_navigation_info).setVisibility(View.GONE);
            view.findViewById(R.id.layout_locked).setVisibility(View.VISIBLE);
        }
    }
}