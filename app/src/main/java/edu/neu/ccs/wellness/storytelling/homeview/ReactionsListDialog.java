package edu.neu.ccs.wellness.storytelling.homeview;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

import edu.neu.ccs.wellness.geostory.FirebaseGeoStoryRepository;
import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.geostory.GeoStoryReaction;
import edu.neu.ccs.wellness.storytelling.R;

/**
 * Created by hermansaksono on 2/15/19.
 */

public class ReactionsListDialog extends DialogFragment {

    public static final String TAG = "ReactionsListDialog";
    public static final String LIST_TEXT = "%s felt %s";
    private String geoStoryId;
    private ListView reactionsListView;

    /* INTERFACE */
    public interface ReactionsListDialogCallback {
        void setReactionsMap(String geoStoryId, Map<String, Integer> geoStoryReactionMap);
        Map<String, Integer> getReactionsMap(String geoStoryId);
    }

    /* FACTORY METHODS */
    public static AlertDialog newInstance(Context context, GeoStory geoStory) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                context, R.style.AppTheme_AlertDialog);
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.dialog_geostory_reactions_list, null);
        ListView reactionsListView = layout.findViewById(R.id.reactions_list_view);
        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<>(context, R.layout.item_reaction);
        reactionsListView.setAdapter(arrayAdapter);
        arrayAdapter.add("Loading...");
        arrayAdapter.notifyDataSetChanged();

        Resources res = context.getResources();
        String dialogTitle = res.getString(
                R.string.geostory_reactions_list_title, geoStory.getUserNickname());

        String[] reactionEmotionNames = context.getResources()
                .getStringArray(R.array.panas_positive_emotion_list);

        getReactions(geoStory, arrayAdapter, reactionEmotionNames);

        builder.setView(layout)
                .setTitle(dialogTitle)
                .setNegativeButton(
                        R.string.geostory_reactions_list_dismiss,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });

        return builder.create();
    }

    private static void getReactions(GeoStory geoStory, final ArrayAdapter<String> adapter,
                                     final String[] reactionEmotionNames) {

        FirebaseGeoStoryRepository.getReactionsFromAGeoStory(geoStory.getStoryId(),
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        adapter.clear();
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot entry : dataSnapshot.getChildren()) {
                                GeoStoryReaction reaction = entry.getValue(GeoStoryReaction.class);
                                String nickname = reaction.getUserNickname();
                                String emotion = reactionEmotionNames[reaction.getReactionId()];

                                adapter.add(String.format(LIST_TEXT, nickname, emotion));
                            }
                        } else {
                            adapter.add("No reactions yet");
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }
}
