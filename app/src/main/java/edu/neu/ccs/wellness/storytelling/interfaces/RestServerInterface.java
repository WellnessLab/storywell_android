package edu.neu.ccs.wellness.storytelling.interfaces;

/**
 * Created by hermansaksono on 6/14/17.
 */

public interface RestServerInterface {
    public UserAuthInterface getUser();

    public String makeGetRequest(String resourcePath);
}
