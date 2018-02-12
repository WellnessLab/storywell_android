package edu.neu.ccs.wellness.fitness.interfaces;

import java.util.List;

import edu.neu.ccs.wellness.fitness.challenges.Challenge;

/**
 * Created by hermansaksono on 10/16/17.
 */

public interface AvailableChallengesInterface {

    String getText();

    String getSubtext();

    List<Challenge> getChallenges();

}