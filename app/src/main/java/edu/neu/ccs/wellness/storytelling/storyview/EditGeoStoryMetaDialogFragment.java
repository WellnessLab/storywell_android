package edu.neu.ccs.wellness.storytelling.storyview;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import edu.neu.ccs.wellness.geostory.GeoStoryMeta;
import edu.neu.ccs.wellness.storytelling.R;

public class EditGeoStoryMetaDialogFragment extends DialogFragment {

    /* CONSTANTS */
    public static final String TAG = "edit_geostory_dialog";

    /* INTERFACE */
    public interface GeoStoryMetaListener {
        void setGeoStoryMeta(GeoStoryMeta geoStoryMeta);
    }

    /* FIELDS */
    private GeoStoryMetaListener listener;
    private GeoStoryMeta meta = new GeoStoryMeta();

    private CheckBox checkBoxShowAvgSteps;
    private CheckBox checkBoxShowNeighborhoods;
    private EditText editTextBio;

    /* FACTORY METHODS */
    public static EditGeoStoryMetaDialogFragment newInstance(GeoStoryMeta meta) {
        EditGeoStoryMetaDialogFragment fragment = new EditGeoStoryMetaDialogFragment();
        fragment.meta = meta;
        return fragment;
    }


    /* OVERRIDE METHODS */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_geostory_edit, null);

        this.checkBoxShowAvgSteps = layout.findViewById(R.id.checkbox_show_avg_steps);
        this.checkBoxShowNeighborhoods = layout.findViewById(R.id.checkbox_show_neighborhood);
        this.editTextBio = layout.findViewById(R.id.edit_text_bio);

        this.checkBoxShowAvgSteps.setChecked(this.meta.isShowAverageSteps());
        this.checkBoxShowNeighborhoods.setChecked(this.meta.isShowNeighborhood());
        this.editTextBio.setText(this.meta.getBio());

        builder.setView(layout)
                .setPositiveButton(R.string.geostory_meta_edit_save,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        saveGeoStoryMeta();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.geostory_meta_edit_dismiss,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    private void saveGeoStoryMeta() {
        this.meta.setShowAverageSteps(this.checkBoxShowAvgSteps.isChecked());
        this.meta.setShowNeighborhood(this.checkBoxShowNeighborhoods.isChecked());
        this.meta.setBio(this.editTextBio.getText().toString());
        this.listener.setGeoStoryMeta(this.meta);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (GeoStoryMetaListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement GeoStoryMetaListener");
        }
    }
}
