package edu.neu.ccs.wellness.storytelling.homeview;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ViewFlipper;

import java.sql.Ref;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import edu.neu.ccs.wellness.fitness.challenges.ChallengeDoesNotExistsException;
import edu.neu.ccs.wellness.fitness.interfaces.ChallengeStatus;
import edu.neu.ccs.wellness.fitness.interfaces.FitnessException;
import edu.neu.ccs.wellness.fitness.interfaces.GroupFitnessInterface;
import edu.neu.ccs.wellness.storytelling.MonitoringActivity;
import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.monitoringview.HeroSprite;
import edu.neu.ccs.wellness.storytelling.monitoringview.MonitoringController;
import edu.neu.ccs.wellness.storytelling.monitoringview.MonitoringView;
import edu.neu.ccs.wellness.storytelling.monitoringview.interfaces.GameLevelInterface;
import edu.neu.ccs.wellness.storytelling.monitoringview.interfaces.OnAnimationCompletedListener;
import edu.neu.ccs.wellness.storytelling.utils.StorywellPerson;
import edu.neu.ccs.wellness.storytelling.viewmodel.RefactoredFitnessSyncViewModel;
import edu.neu.ccs.wellness.storytelling.viewmodel.SyncStatus;
import edu.neu.ccs.wellness.storytelling.viewmodel.FetchingStatus;
import edu.neu.ccs.wellness.storytelling.viewmodel.FirebaseFitnessChallengeViewModel;
import edu.neu.ccs.wellness.storytelling.viewmodel.FitnessSyncViewModel;
import edu.neu.ccs.wellness.utils.WellnessDate;
import edu.neu.ccs.wellness.utils.WellnessReport;

/**
 * Created by hermansaksono on 6/11/18.
 */

public class HomeAdventurePresenter {

    private GregorianCalendar today;
    private GregorianCalendar startDate;
    private GregorianCalendar endDate;
    private ProgressAnimationStatus progressAnimationStatus = ProgressAnimationStatus.UNSTARTED;
    private boolean isSyncronizingFitnessData = false;
    //private boolean isProgressAnimationCompleted = false;

    private View rootView;
    private ViewFlipper viewFlipper;
    private FloatingActionButton fabPlay;
    private FloatingActionButton fabCalendarShow;
    private FloatingActionButton fabCalendarHide;
    private Snackbar currentSnackbar;

    //private FamilyFitnessChallengeViewModel familyFitnessChallengeViewModel;
    private FirebaseFitnessChallengeViewModel familyFitnessChallengeViewModel;
    //private FitnessSyncViewModel fitnessSyncViewModel;
    private RefactoredFitnessSyncViewModel fitnessSyncViewModel;
    private MonitoringController gameController;
    private MonitoringView gameView;

    private static final int FIRST_DAY_OF_WEEK = Calendar.SUNDAY;

    public HomeAdventurePresenter(View rootView) {
        /* Basic data */
        this.today = WellnessDate.getTodayDate();
        //this.today = getDummyDate(); // TODO REMOVE THIS FOR PRODUCTION
        this.startDate = WellnessDate.getFirstDayOfWeek(this.today);
        this.endDate = WellnessDate.getEndDate(this.startDate);

        /* Views */
        this.rootView = rootView;
        this.viewFlipper = this.rootView.findViewById(R.id.view_flipper);
        this.fabPlay = this.rootView.findViewById(R.id.fab_action);
        this.fabPlay.setVisibility(View.INVISIBLE);
        this.fabCalendarShow = this.rootView.findViewById(R.id.fab_show_calendar);
        this.fabCalendarHide = this.rootView.findViewById(R.id.fab_seven_day_close);

        /* Game's Controller */
        Typeface gameFont = ResourcesCompat.getFont(rootView.getContext(), MonitoringActivity.FONT_FAMILY);
        GameLevelInterface gameLevel = MonitoringActivity.getGameLevelDesign(gameFont);
        HeroSprite hero = new HeroSprite(rootView.getResources(), R.drawable.hero_dora,
                MonitoringActivity.getAdultBalloonDrawables(10),
                MonitoringActivity.getChildBalloonDrawables(10),
                R.color.colorPrimaryLight);
        this.gameView = rootView.findViewById(R.id.layout_monitoringView);
        this.gameController = new MonitoringController(this.gameView);
        this.gameController.setLevelDesign(rootView.getResources(), gameLevel);
        this.gameController.setHeroSprite(hero);
    }

    /* BUTTON METHODS */
    public void onFabPlayClicked(Activity activity) {
        if (this.progressAnimationStatus == ProgressAnimationStatus.UNSTARTED) {
            tryStartProgressAnimation(activity);
        } else if (this.progressAnimationStatus == ProgressAnimationStatus.PLAYING) {
            // do nothing
        } else if (this.progressAnimationStatus == ProgressAnimationStatus.COMPLETED) {
            resetProgressAnimation(activity);
        }
    }

    public void onFabShowCalendarClicked(View view) {
        viewFlipper.setInAnimation(view.getContext(), R.anim.overlay_move_down);
        viewFlipper.setOutAnimation(view.getContext(), R.anim.basecard_move_down);
        viewFlipper.showNext();
    }

    public void onFabCalendarHideClicked(View view) {
        viewFlipper.setInAnimation(view.getContext(), R.anim.basecard_move_up);
        viewFlipper.setOutAnimation(view.getContext(), R.anim.overlay_move_up);
        viewFlipper.showPrevious();
    }

    private void setFabPlayToOriginal() {
        this.fabPlay.setImageResource(R.drawable.ic_round_play_arrow_full_24px);
    }

    private void setFabPlayToRewind() {
        this.fabPlay.setImageResource(R.drawable.ic_round_replay_24px);
    }

    /* GAMEVIEW METHODS */
    public void startGameView() {
        this.gameController.start();
    }

    public void resumeGameView() {
        this.gameController.resume();
    }

    public void pauseGameView() {
        this.gameController.pause();
    }

    public void stopGameView() {
        this.gameController.stop();
    }

    public boolean processTapOnGameView(Activity activity, MotionEvent event) {
        if (this.isFitnessAndChallengeDataReady()) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (this.gameView.isOverHero(event)) {
                    this.tryStartProgressAnimation(activity);
                }
            }
        }
        return true;
    }

    /* PROGRESS ANIMATION STATES */
    enum ProgressAnimationStatus {
        UNSTARTED, PLAYING, COMPLETED;
    }

    /* PROGRESS ANIMATION METHODS */
    private void doPrepareProgressAnimations(Activity activity) {
        try {
            if (this.isChallengeStatusReadyForAdventure()) {
                this.fabPlay.show();
                this.showProgressAnimationInstructionSnackbar(activity);
            } else {
                this.fabPlay.show();
                this.showProgressAnimationInstructionSnackbar(activity);
                /*
                this.showNoAdventureMessage(activity);
                this.gameController.setHeroIsVisible(false); // TODO Uncomment on production
                */
            }
        } catch (ChallengeDoesNotExistsException e) {
            e.printStackTrace();
            Log.e("SWELL", e.getMessage());
        }
    }

    private void tryStartProgressAnimation(Activity activity) {
        if (isFitnessAndChallengeDataFetched()) {
            this.dismissSnackbar();
            startProgressAnimation(activity);
        } else {
            // DON'T DO ANYTHING
        }
    }

    private void startProgressAnimation(final Activity activity) {
        try {
            float adultProgress = this.familyFitnessChallengeViewModel.getAdultProgress(today);
            float childProgress = this.familyFitnessChallengeViewModel.getChildProgress(today);
            float overallProgress = this.familyFitnessChallengeViewModel.getOverallProgress(today);
            this.gameController.setProgress(adultProgress, childProgress, overallProgress, new OnAnimationCompletedListener() {
                @Override
                public void onAnimationCompleted() {
                    showPostProgressAnimationMessage(activity);
                    setFabPlayToRewind();
                    progressAnimationStatus = ProgressAnimationStatus.COMPLETED;
                }
            });
            this.progressAnimationStatus = ProgressAnimationStatus.PLAYING;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetProgressAnimation(Activity activity) {
        this.progressAnimationStatus = ProgressAnimationStatus.UNSTARTED;
        this.gameController.resetProgress();
        this.showProgressAnimationInstructionSnackbar(activity);
        this.setFabPlayToOriginal();
    }

    /* FITNESS SYNC VIEWMODEL METHODS */
    public boolean trySyncFitnessData(Fragment fragment) {
        if (isSyncronizingFitnessData) {
            return false;
        } else {
            this.isSyncronizingFitnessData = true;
            this.syncFitnessData(fragment);
            return true;
        }
    }

    private void syncFitnessData(final Fragment fragment) {
        Storywell storywell = new Storywell(fragment.getContext());
        this.fitnessSyncViewModel = ViewModelProviders.of(fragment).get(RefactoredFitnessSyncViewModel.class);
        //this.fitnessSyncViewModel = ViewModelProviders.of(fragment).get(FitnessSyncViewModel.class);
        this.fitnessSyncViewModel
                .perform(storywell.getGroup())
                .observe(fragment, new Observer<SyncStatus>(){

                    @Override
                    public void onChanged(@Nullable SyncStatus syncStatus) {
                        onSyncStatusChanged(syncStatus);
                    }
                });
    }

    private void onSyncStatusChanged(SyncStatus syncStatus) {
        if (SyncStatus.CONNECTING.equals(syncStatus)) {
            Log.d("SWELL", "Connecting: " + getCurrentPersonString());
        } else if (SyncStatus.DOWNLOADING.equals(syncStatus)) {
            Log.d("SWELL", "Downloading fitness data: " + getCurrentPersonString());
        } else if (SyncStatus.UPLOADING.equals(syncStatus)) {
            Log.d("SWELL", "Uploading fitness data: " + getCurrentPersonString());
        } else if (SyncStatus.IN_PROGRESS.equals(syncStatus)) {
            Log.d("SWELL", "Sync completed for: " + getCurrentPersonString());
            this.fitnessSyncViewModel.performNext();
        } else if (SyncStatus.SUCCESS.equals(syncStatus)) {
            this.isSyncronizingFitnessData = false;
            Log.d("SWELL", "All sync successful!");
        } else if (SyncStatus.FAILED.equals(syncStatus)) {
            this.isSyncronizingFitnessData = false;
            Log.d("SWELL", "Sync failed");
        }
    }

    private String getCurrentPersonString() {
        StorywellPerson person = fitnessSyncViewModel.getCurrentPerson();
        if (person != null) {
            return person.toString();
        } else {
            return "Null Person";
        }
    }

    /* FITNESS CHALLENGE VIEWMODEL METHODS */
    public void tryFetchChallengeAndFitnessData(Fragment fragment) {
        if (this.isFitnessAndChallengeDataReady() == false){
            this.fetchChallengeAndFitnessData(fragment);
        }
    }

    public void fetchChallengeAndFitnessData(Fragment fragment) {
        this.familyFitnessChallengeViewModel = getFamilyFitnessChallengeViewModel(fragment);
        this.showDownloadingFitnessDataMessage(fragment.getActivity());
    }

    private FirebaseFitnessChallengeViewModel getFamilyFitnessChallengeViewModel (final Fragment fragment) {
        //FamilyFitnessChallengeViewModel viewModel;
        FirebaseFitnessChallengeViewModel viewModel;
        viewModel = ViewModelProviders.of(fragment).get(FirebaseFitnessChallengeViewModel.class);
        viewModel.fetchSevenDayFitness(startDate, endDate).observe(fragment, new Observer<FetchingStatus>() {
            @Override
            public void onChanged(@Nullable final FetchingStatus status) {
                if (status == FetchingStatus.SUCCESS) {
                    Log.d("SWELL", "Fitness data fetched");
                    doPrepareProgressAnimations(fragment.getActivity());
                    printFitnessData();
                } else if (status == FetchingStatus.NO_INTERNET) {
                    Log.e("SWELL", "No internet connection to fetch fitness challenges.");
                    showNoInternetMessage(fragment);
                } else if (status == FetchingStatus.FETCHING) {
                    // DO NOTHING
                } else {
                    Log.e("SWELL", "Fetching fitness challenge failed: " + status.toString());
                    showSystemSideErrorMessage(fragment.getActivity());
                }
            }
        });
        return viewModel;
    }

    public boolean isFitnessAndChallengeDataReady() {
        if (this.familyFitnessChallengeViewModel == null) {
            return false;
        } else {
            return this.isFitnessAndChallengeDataFetched();
        }
    }
    public boolean isFitnessAndChallengeDataFetched() {
        return (familyFitnessChallengeViewModel.getFetchingStatus() == FetchingStatus.SUCCESS);
    }

    public boolean isChallengeStatusReadyForAdventure() throws ChallengeDoesNotExistsException {
        if (isFitnessAndChallengeDataReady()) {
            ChallengeStatus status = this.familyFitnessChallengeViewModel.getChallengeStatus();
            return status == ChallengeStatus.UNSYNCED_RUN || status == ChallengeStatus.RUNNING;
        } else {
            return false;
        }
    }

    /* SNACKBAR METHODS */
    public void dismissSnackbar() {
        if (this.currentSnackbar != null) {
            this.currentSnackbar.dismiss();
        }
    }

    public void showSystemSideErrorMessage(final Activity activity) {
        String instruction = activity.getString(R.string.tooltip_snackbar_northeastern_error);
        final String errorString = activity.getString(R.string.sms_error_body);
        this.currentSnackbar = getSnackbar(instruction, activity);
        this.currentSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
        this.currentSnackbar.setAction(R.string.button_text_us, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //currentSnackbar.dismiss();
                WellnessReport.sendSMS(activity.getApplication(),"8572456328", errorString);
            }
        });
        this.currentSnackbar.show();
    }

    public void showProgressAnimationInstructionSnackbar(Activity activity) {
        String instruction = activity.getString(R.string.tooltip_see_monitoring_progress);
        this.currentSnackbar = getSnackbar(instruction, activity);
        this.currentSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
        this.currentSnackbar.show();
    }

    public void showPostProgressAnimationMessage(final Activity activity) {
        String message = activity.getString(R.string.tooltip_snackbar_progress_ongoing);
        this.currentSnackbar = getSnackbar(message, activity);
        this.currentSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
        this.currentSnackbar.setAction(R.string.button_adventure_refresh, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSnackbar.dismiss();
                resetProgressAnimation(activity);
            }
        });
        this.currentSnackbar.show();
    }

    public void showDownloadingFitnessDataMessage(Activity activity) {
        String message = activity.getString(R.string.tooltip_snackbar_downloading_fitness_data);
        this.currentSnackbar = getSnackbar(message, activity);
        this.currentSnackbar.show();
    }

    public void showNoAdventureMessage(Activity activity) {
        String instruction = activity.getString(R.string.tooltip_snackbar_no_running_challenge);
        this.currentSnackbar = getSnackbar(instruction, activity);
        this.currentSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
        this.currentSnackbar.show();
    }

    public void showNoInternetMessage(final Fragment fragment) {
        String instruction = fragment.getString(R.string.tooltip_snackbar_adventure_no_internet);
        this.currentSnackbar = getSnackbar(instruction, fragment.getActivity());
        this.currentSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
        this.currentSnackbar.setAction(R.string.button_adventure_refresh, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSnackbar.dismiss();
                tryFetchChallengeAndFitnessData(fragment);
            }
        });
        this.currentSnackbar.show();
    }

    /* STATIC SNACKBAR METHODS*/
    public static Snackbar getSnackbar(String text, Activity activity) {
        View gameView = activity.findViewById(R.id.layout_gameview);
        Snackbar snackbar = Snackbar.make(gameView, text, Snackbar.LENGTH_LONG);
        snackbar = setSnackBarTheme(snackbar, activity.getApplicationContext());
        return snackbar;
    }

    private static Snackbar setSnackBarTheme(Snackbar snackbar, Context context) {
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(context, R.color.sea_foregroundDark));
        snackbarView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return snackbar;
    }

    /* DUMMY DATA */
    private static GregorianCalendar getDummyDate() {
        GregorianCalendar calendar = (GregorianCalendar) Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.set(Calendar.YEAR, 2018);
        calendar.set(Calendar.MONTH, Calendar.JULY);
        calendar.set(Calendar.DAY_OF_MONTH, 20);
        calendar.set(Calendar.HOUR_OF_DAY, 16);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private void printFitnessData() {
        try {
            GroupFitnessInterface groupFitness = familyFitnessChallengeViewModel.getSevenDayFitness();
            Log.d("SWELL", groupFitness.toString());
        } catch (FitnessException e) {
            e.printStackTrace();
        }
    }
}
