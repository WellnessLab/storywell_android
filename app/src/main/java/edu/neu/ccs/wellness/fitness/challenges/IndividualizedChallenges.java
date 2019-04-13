package edu.neu.ccs.wellness.fitness.challenges;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import edu.neu.ccs.wellness.fitness.interfaces.AvailableChallengesInterface;

public class IndividualizedChallenges  implements AvailableChallengesInterface {
    private static final String STRING_FORMAT = "%s - %s";

    @SerializedName("start_datetime")
    private String startDatetimeUtcString;

    @SerializedName("total_duration")
    private String totalDurationString;

    @SerializedName("level_id")
    private int levelId;

    private String text;
    private String subtext;
    private List<UnitChallenge> challenges = null;

    @SerializedName("challenges_by_person")
    private Map<String, List<UnitChallenge>> challengesByPerson;

    private IndividualizedChallenges() {

    }

    /* STATIC FACTORY METHOD */
    public static IndividualizedChallenges newInstance(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, IndividualizedChallenges.class);
    }


    @Override
    public String getText() { return this.text; }

    @Override
    public String getSubtext() { return this.subtext; }

    @Override
    public String toString() {
        return String.format(STRING_FORMAT, this.text, this.subtext);
    }

    @Override
    public List<UnitChallenge> getChallenges() {
        return this.challenges;
    }

    @Override
    public Map<String, List<UnitChallenge>> getChallengesByPerson() {
        return this.challengesByPerson;
    }

    public IndividualizedChallengesToPost getPostingInstance() {
        return new IndividualizedChallengesToPost(
                this.startDatetimeUtcString, this.totalDurationString, this.levelId);
    }
}
