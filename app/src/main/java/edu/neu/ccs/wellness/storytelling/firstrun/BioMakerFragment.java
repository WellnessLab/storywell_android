package edu.neu.ccs.wellness.storytelling.firstrun;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSetting;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BioMakerFragment#newInstance} factory method to
 * newInstance an instance of this fragment.
 */
public class BioMakerFragment extends Fragment implements View.OnClickListener {
    public static final String KEY_FAMILY_BIO = "KEY_FAMILY_BIO";
    private static final int VIEW_BIO_CREATOR = 0;
    private static final int VIEW_BIO_SUBMIT = 1;

    // private FusedLocationProviderClient locationProvider;
    private OnBioUpdateListener onBioUpdateListener;

    private ViewAnimator bioCreatorViewAnimator;
    private String nickname;
    private int age;
    private int numOfChildren;
    private String hobby;
    private String bioText;
    private TextView bioEditText;
    private Location homeLocation;

    public interface OnBioUpdateListener {
        void onBioSaved(SynchronizedSetting.FamilyInfo familyInfo);
    }

    public BioMakerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to newInstance a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AppIntroductionFragment.
     */
    public static BioMakerFragment newInstance() {
        return new BioMakerFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.initLocationListener();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_firstrun_bio, container, false);
        this.bioCreatorViewAnimator = view.findViewById(R.id.viewanimator_bio_creator);
        this.bioCreatorViewAnimator.setInAnimation(getContext(), R.anim.reflection_fade_in);
        this.bioCreatorViewAnimator.setOutAnimation(getContext(), R.anim.reflection_fade_out);

        this.bioEditText = view.findViewById(R.id.bio_final_edit_text);

        view.findViewById(R.id.make_bio_button).setOnClickListener(this);
        view.findViewById(R.id.save_bio_button).setOnClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.onBioUpdateListener = (OnBioUpdateListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(((Activity) context).getLocalClassName()
                    + " must implement OnBioUpdateListener");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.make_bio_button:
                makeBioAndContinue();
                break;
            case R.id.save_bio_button:
                saveBioAndNext();
                break;
        }
    }

    private void makeBioAndContinue() {
        /*
        this.nickname = getStringFromFieldId(getView(), R.id.bio_nickname_edit_text);
        this.age = getIntFromFieldId(getView(), R.id.bio_age_edit_text);
        this.numOfChildren = getIntFromFieldId(getView(), R.id.bio_children_edit_text);
        this.hobby = getStringFromFieldId(getView(), R.id.bio_hobby_edit_text);
        */

        boolean isError = false;

        EditText nicknameEditText = getView().findViewById(R.id.bio_nickname_edit_text);
        if (nicknameEditText.getText().toString().isEmpty()) {
            nicknameEditText.setError("Please type your name for this app.");
            isError = true;
        } else {
            this.nickname = nicknameEditText.getText().toString();
        }

        EditText ageEditText = getView().findViewById(R.id.bio_age_edit_text);
        if (ageEditText.getText().toString().isEmpty()) {
            ageEditText.setError("Please type your age.");
            isError = true;
        } else {
            this.age = Integer.valueOf(ageEditText.getText().toString());
        }

        EditText numChildrenEditText = getView().findViewById(R.id.bio_children_edit_text);
        if (numChildrenEditText.getText().toString().isEmpty()) {
            numChildrenEditText.setError("Please type how many kids you have.");
            isError = true;
        } else {
            this.numOfChildren = Integer.valueOf(numChildrenEditText.getText().toString());
        }

        EditText hobbyEditText = getView().findViewById(R.id.bio_hobby_edit_text);
        if (hobbyEditText.getText().toString().isEmpty()) {
            hobbyEditText.setError("Please type your hobby.");
            isError = true;
        } else {
            this.hobby = hobbyEditText.getText().toString();
        }

        if (!isError) {
            this.bioText = generateBioText(this.nickname, this.age, this.numOfChildren, this.hobby);
            this.bioEditText.setText(this.bioText);
            this.bioCreatorViewAnimator.setDisplayedChild(VIEW_BIO_SUBMIT);
        }
    }

    private void saveBioAndNext() {
        SynchronizedSetting.FamilyInfo familyInfo = new SynchronizedSetting.FamilyInfo();
        familyInfo.setCaregiverNickname(nickname);
        familyInfo.setApproximateAge(age);
        familyInfo.setNumberOfChildren(numOfChildren);
        familyInfo.setCaregiverBio(bioText);

        if (this.homeLocation != null) {
            familyInfo.setHomeLatitude(this.homeLocation.getLatitude());
            familyInfo.setHomeLongitude(this.homeLocation.getLongitude());
        }

        this.onBioUpdateListener.onBioSaved(familyInfo);
    }

    private String generateBioText(String nickname, int age, int numOfChildren, String hobby) {
        Resources res = getResources();
        int roundedAge = (int) (Math.floor(age / 10.0f) * 10);
        String numOfChildrenStr = res.getQuantityString(R.plurals.number_of_children
                , numOfChildren, numOfChildren);
        return res.getString(R.string.default_bio_template, roundedAge, numOfChildrenStr, hobby);
    }

    private void initLocationListener() {
        FusedLocationProviderClient locationProvider =
                LocationServices.getFusedLocationProviderClient(getActivity());

        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationProvider.getLastLocation().addOnSuccessListener(
                    this.getActivity(), locationListener);
        }
    }

    /**
     * Listener to handle the GPS location fetching.
     */
    private OnSuccessListener<Location> locationListener = new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(final Location location) {
            if (location != null) {
                homeLocation = location;
            }
        }
    };

    /* HELPER METHODS */
    private static String getStringFromFieldId(View view, int resId) {
        return ((TextView) view.findViewById(resId)).getText().toString();
    }

    private static int getIntFromFieldId(View view, int resId) {
        return Integer.valueOf((getStringFromFieldId(view, resId)));
    }

 }