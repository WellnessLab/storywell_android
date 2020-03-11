package edu.neu.ccs.wellness.storytelling.utils;

public class PlaceItem {
    public String name;
    public String displayName;
    public double lat;
    public double lng;
    public String address;
    public boolean isCurrentLocation = false;

    public PlaceItem(String name, double lat, double lng) {
        this.name = name;
        this.displayName = name;
        this.lat = lat;
        this.lng = lng;
    }
}
