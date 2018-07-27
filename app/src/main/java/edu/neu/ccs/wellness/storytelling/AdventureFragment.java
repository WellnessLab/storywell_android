package edu.neu.ccs.wellness.storytelling;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import edu.neu.ccs.wellness.storytelling.homeview.HomeAdventurePresenter;

public class AdventureFragment extends Fragment {

    /* PRIVATE VARIABLES */
    private HomeAdventurePresenter presenter;

    /* CONSTRUCTOR */
    public AdventureFragment() {
        // Required empty public constructor
    }

    /* FACTORY METHOD */
    public static AdventureFragment newInstance() {
        return new AdventureFragment();
    }

    /* INTERFACE FUNCTIONS */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_adventure, container, false);

        /* Prepare the Presenter */
        this.presenter = new HomeAdventurePresenter(rootView);

        // Set up GameView's OnTouch event
        rootView.findViewById(R.id.layout_monitoringView).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return presenter.processTapOnGameView(event);
            }
        });

        // Set up control button for starting animation
        rootView.findViewById(R.id.button_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.startSyncAndShowProgressAnimation(view);
            }
        });

        // Set up control button for playing vis animation
        rootView.findViewById(R.id.button_go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.startProgressAnimation();
                presenter.showControlForProgressInfo(view);
            }
        });

        // Set up control button to show first control card
        rootView.findViewById(R.id.button_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.resetProgressAnimation();
                presenter.showControlForFirstCard(view);
            }
        });

        // Set up control button to show first prev/next card
        rootView.findViewById(R.id.button_go_prev_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.showControlForPrevNext(view);
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUserVisibleHint(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.presenter.startGameView();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.presenter.tryFetchFitnessChallengeData(this);
        this.presenter.trySyncFitnessData(this);
        this.presenter.resumeGameView();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.presenter.pauseGameView();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.presenter.stopGameView();
    }

    /* MONITORING ACTIVITY */
    private void startMonitoringActivity() {
        Intent intent = new Intent(getContext(), MonitoringActivity.class);
        getContext().startActivity(intent);
    }
}
