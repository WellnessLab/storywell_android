package edu.neu.ccs.wellness.storytelling;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.neu.ccs.wellness.tracking.Event;
import edu.neu.ccs.wellness.tracking.Param;
import edu.neu.ccs.wellness.tracking.UserTrackDetails;
import edu.neu.ccs.wellness.tracking.WellnessUserTracking;

public class AboutActivity extends AppCompatActivity {

    private WellnessUserTracking wellnessUserTracking;
    private Bundle eventParams;
    private Storywell storywell;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);


        storywell = new Storywell(getApplicationContext());
        wellnessUserTracking = storywell.getUserTracker("108");
        eventParams = new Bundle();
        eventParams.putString(Param.ACTIVITY_NAME,"AboutActivity");
        wellnessUserTracking.logEvent(Event.ACTIVITY_OPEN, eventParams);

    }
}
