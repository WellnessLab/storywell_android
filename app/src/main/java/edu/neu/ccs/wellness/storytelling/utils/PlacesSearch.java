package edu.neu.ccs.wellness.storytelling.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlacesSearch {

    private static final String BASE_URL =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
    private static final String LOC_FORMAT = "location=%g,%g";
    private static final String RADIUS_FORMAT = "radius=%d";
    private static final String TYPE_FORMAT = "type=%s";
    private static final String API_FORMAT = "key=%s";
    private static final String RANK_BY_DISTANCE = "rankby=distance";
    private static final String AMP = "&";
    private static final String KEYWORD_FORMAT = "keyword=%s";

    /* FIELDS */
    private Context context;
    private String googleApiKey;

    public PlacesSearch(Context context) {
        this.context = context.getApplicationContext();
        this.googleApiKey = getGoogleApiKey(context);
    }

    public List<PlaceItem> getNearby(double lat, double lng, int radius, String type) {
        List<PlaceItem> nearbyPlaces = new ArrayList<>();

        try {
            URL url = getUrlForNearby(lat, lng, radius, type);
            String responseStr = doGetRequest(url);
            JSONObject response = new JSONObject(responseStr);
            nearbyPlaces = getNearbyFromJson(response);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return nearbyPlaces;
    }

    public List<PlaceItem> getByKeyword(double lat, double lng, int radius, String keyword) {
        List<PlaceItem> placesResult = new ArrayList<>();

        try {
            URL url = getUrlForKeyword(lat, lng, radius, keyword);
            String responseStr = doGetRequest(url);
            JSONObject response = new JSONObject(responseStr);
            placesResult = getNearbyFromJson(response);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return placesResult;
    }

    private List<PlaceItem> getNearbyFromJson(JSONObject json) throws JSONException {
        List<PlaceItem> nearbyPlaces = new ArrayList<>();

        JSONArray jsonPlaces = json.getJSONArray("results");

        for (int i = 0; i < jsonPlaces.length(); i++) {
            JSONObject jsonPlace = jsonPlaces.getJSONObject(i);
            JSONObject jsonLocation = jsonPlace.getJSONObject("geometry")
                    .getJSONObject("location");
            Double lat = jsonLocation.getDouble("lat");
            Double lng = jsonLocation.getDouble("lng");
            PlaceItem placeItem = new PlaceItem(jsonPlace.optString("name"), lat, lng);

            nearbyPlaces.add(placeItem);
        }

        return nearbyPlaces;
    }

    private URL getUrlForNearby(double lat, double lng, int radius, String type)
            throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        sb.append(BASE_URL);
        sb.append(String.format(Locale.US, LOC_FORMAT, lat, lng)).append(AMP);
        //sb.append(String.format(Locale.US, RADIUS_FORMAT, radius)).append(AMP);
        sb.append(RANK_BY_DISTANCE).append(AMP);
        sb.append(String.format(Locale.US, TYPE_FORMAT, type)).append(AMP);
        sb.append(String.format(Locale.US, API_FORMAT, this.googleApiKey));
        return new URL(sb.toString());
    }

    private URL getUrlForKeyword(double lat, double lng, int radius, String keyword)
            throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        sb.append(BASE_URL);
        sb.append(String.format(Locale.US, LOC_FORMAT, lat, lng)).append(AMP);
        sb.append(String.format(Locale.US, RADIUS_FORMAT, radius)).append(AMP);
        //sb.append(RANK_BY_DISTANCE).append(AMP);
        sb.append(String.format(Locale.US, KEYWORD_FORMAT, keyword)).append(AMP);
        sb.append(String.format(Locale.US, API_FORMAT, this.googleApiKey));
        return new URL(sb.toString());
    }

    private String doGetRequest(URL url) throws IOException {
        String output = null;
        BufferedReader bufferedReader = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(
                        "Error " + connection.getResponseCode()+ ": " + url.toString());
            } else {
                String result;
                StringBuilder resultBuilder = new StringBuilder();

                bufferedReader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                while ((result = bufferedReader.readLine()) != null) {
                    resultBuilder.append(result);
                }
                bufferedReader.close();
                output = resultBuilder.toString();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return output;
    }

    public static String getGoogleApiKey(Context context) {
        String apiKey = "";
        try {
            Bundle metadata = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            apiKey = metadata.getString("com.google.android.geo.API_KEY", "");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return apiKey;
    }
}
