package edu.neu.ccs.wellness.storytelling.storyview;

import android.app.Dialog;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.appcompat.widget.Toolbar;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import edu.neu.ccs.wellness.geostory.GeoStoryMeta;
import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.homeview.GeoStoryIcons;

public class EditGeoStoryMetaDialogFragment
        extends DialogFragment
        implements AdapterView.OnItemSelectedListener {

    /* CONSTANTS */
    public static final String TAG = "edit_geostory_dialog";

    /* INTERFACE */
    public interface GeoStoryMetaListener {
        void setEditGeoStoryMeta(GeoStoryMeta geoStoryMeta);
    }

    /* FIELDS */
    private GeoStoryMetaListener listener;
    private GeoStoryMeta meta = new GeoStoryMeta();

    private Toolbar toolbar;
    private EditText editTextBio;
    private Spinner spinner;
    private CheckBox checkBoxShowAvgSteps;
    private CheckBox checkBoxShowNeighborhoods;

    private int selectedIcon = 0;
    private int highestAvailableIcon = 2;

    private LayoutInflater inflater;

    /* FACTORY METHODS */
    public static EditGeoStoryMetaDialogFragment newInstance(GeoStoryMeta meta,
                                                             int highestAvailableIcon) {
        EditGeoStoryMetaDialogFragment fragment = new EditGeoStoryMetaDialogFragment();
        fragment.meta = meta;
        fragment.highestAvailableIcon = highestAvailableIcon;
        fragment.selectedIcon = highestAvailableIcon;
        return fragment;
    }

    /* OVERRIDE METHODS */
    @Override
    //public Dialog onCreateDialog(Bundle savedInstanceState) {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //LayoutInflater inflater = getActivity().getLayoutInflater();
        //View layout = inflater.inflate(R.layout.dialog_geostory_edit, null);
        View layout = inflater.inflate(R.layout.dialog_geostory_edit, container, false);

        this.inflater = inflater;

        this.toolbar = layout.findViewById(R.id.toolbar);
        this.checkBoxShowAvgSteps = layout.findViewById(R.id.checkbox_show_avg_steps);
        this.checkBoxShowNeighborhoods = layout.findViewById(R.id.checkbox_show_neighborhood);
        this.editTextBio = layout.findViewById(R.id.edit_text_bio);
        this.spinner = layout.findViewById(R.id.spinner_story_icon);
        this.spinner.setAdapter(new GeostoryIconAdapter());
        this.spinner.setSelection(this.selectedIcon);
        this.spinner.setOnItemSelectedListener(this);

        this.checkBoxShowAvgSteps.setChecked(this.meta.isShowAverageSteps());
        this.checkBoxShowNeighborhoods.setChecked(this.meta.isShowNeighborhood());
        this.editTextBio.setText(this.meta.getBio());

        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        toolbar.setTitle("Edit Story Info");
        toolbar.inflateMenu(R.menu.edit_geostory);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_save_geostory_info:
                        saveGeoStoryMeta();
                        dismiss();
                        break;
                    default:
                        dismiss();
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setWindowAnimations(R.style.AppTheme_Slide);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        this.selectedIcon = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }

    private void saveGeoStoryMeta() {
        this.meta.setShowAverageSteps(this.checkBoxShowAvgSteps.isChecked());
        this.meta.setShowNeighborhood(this.checkBoxShowNeighborhoods.isChecked());
        this.meta.setBio(this.editTextBio.getText().toString());
        this.meta.setIconId(this.selectedIcon);

        try {
            this.listener = (GeoStoryMetaListener) getTargetFragment();
            this.listener.setEditGeoStoryMeta(this.meta);
        } catch (ClassCastException e) {
            throw new ClassCastException(getTargetFragment().toString()
                    + " must implement GeoStoryMetaListener");
        }
    }

    /* CUSTOM CLASSES */
    class GeostoryIconAdapter extends BaseAdapter {

        String[] iconNames = getResources().getStringArray(R.array.geostory_icon_name);

        @Override
        public int getCount() {
            return GeoStoryIcons.NUM_ICONS;
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_geostory_icon_spinner, null);
            }
            ImageView iconView = convertView.findViewById(R.id.icon_src_geostory);
            TextView textView = convertView.findViewById(R.id.icon_name_geostory);

            String text = iconNames[position];
            if (!isEnabled(position)) {
                text = text.concat(" \uD83D\uDD12");
            }

            iconView.setImageResource(GeoStoryIcons.ICONS[position]);
            textView.setText(text);

            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            return position <= highestAvailableIcon;
        }

    }
}
