package edu.neu.ccs.wellness.fitness.interfaces;

import android.content.Context;

import java.net.ConnectException;

import edu.neu.ccs.wellness.fitness.challenges.Challenge;
import edu.neu.ccs.wellness.fitness.challenges.RunningChallenge;
import edu.neu.ccs.wellness.server.RestServer;

/**
 * Created by hermansaksono on 2/5/18.
 */

public interface ChallengeManagerInterface {

    /**
     * Return the user's current ChallengeStatus. If there is no saved challenge in internal storage
     * then return ChallengeStatus.UNITIALIZED
     * @return The status of user's Challenge
     */
    ChallengeStatus getStatus();

    /**
     * Get the a list of available challenges if the ChallengeStatus is either UNSTARTED or AVAILABLE.
     * @return Available challenges
     */
    AvailableChallengesInterface getAvailableChallenges();

    /**
     * Set the running challenge if the ChallengeStatus is AVAILABLE. Then sets the status to
     * UNSYNCED_RUN. This function MUST save the given challenge to a persistent storage.
     * It should not sync the given challenge to server.
     * @param challenge
     */
    void setRunningChallenge(Challenge challenge);

    /**
     * Get the currently unsynced running Challenge if the ChallengeStatus is UNSYNCED_RUN.
     * @return Currently running but unsynced challenge
     */
    Challenge getUnsyncedChallenge();

    /**
     * Get the currently running Challenge if the ChallengeStatus is RUNNING.
     * @return Currently running challenge
     */
    RunningChallenge getRunningChallenge();

    /**
     * Sync the running challenge (that was saved in the persistent storage) with the server.
     * @return The status of the synchronization
     */
    RestServer.ResponseType syncRunningChallenge();

    /**
     *
     */
    void manageChallenge();

    void completeChallenge();

    void syncCompletedChallenge();

    void changeChallengeStatus(int state) throws Exception;
}
