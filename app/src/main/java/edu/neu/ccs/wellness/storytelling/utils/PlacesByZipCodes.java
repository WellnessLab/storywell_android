package edu.neu.ccs.wellness.storytelling.utils;

import java.util.HashMap;
import java.util.Map;

public class PlacesByZipCodes {

    private Map<String, String> zipNeighborhoodMap = new HashMap<>();

    public PlacesByZipCodes() {
        zipNeighborhoodMap.put("02101",	"Downtown Boston");
        zipNeighborhoodMap.put("02108",	"Beacon Hill");
        zipNeighborhoodMap.put("02109",	"Inner Harbor");
        zipNeighborhoodMap.put("02110",	"Financial District");
        zipNeighborhoodMap.put("02111",	"Chinatown");
        zipNeighborhoodMap.put("02116",	"Back Bay");
        zipNeighborhoodMap.put("02117",	"Downtown Boston");
        zipNeighborhoodMap.put("02118",	"South End");
        zipNeighborhoodMap.put("02119",	"Roxbury");
        zipNeighborhoodMap.put("02120",	"Roxbury Crossing");
        zipNeighborhoodMap.put("02121",	"Grove Hall");
        zipNeighborhoodMap.put("02122",	"Dorchester");
        zipNeighborhoodMap.put("02123",	"Downtown Boston");
        zipNeighborhoodMap.put("02124",	"Dorchester");
        zipNeighborhoodMap.put("02125",	"Dorchester");
        zipNeighborhoodMap.put("02126",	"Mattapan");
        zipNeighborhoodMap.put("02127",	"South Boston");
        zipNeighborhoodMap.put("02128",	"East Boston");
        zipNeighborhoodMap.put("02129",	"Charlestown");
        zipNeighborhoodMap.put("02130",	"Jamaica Plain");
        zipNeighborhoodMap.put("02131",	"Roslindale");
        zipNeighborhoodMap.put("02132",	"West Roxbury");
        zipNeighborhoodMap.put("02134",	"Allston");
        zipNeighborhoodMap.put("02135",	"Brighton");
        zipNeighborhoodMap.put("02136",	"Hyde Park");
        zipNeighborhoodMap.put("02210",	"East Boston");
        zipNeighborhoodMap.put("02137",	"Readville");
        zipNeighborhoodMap.put("02138",	"Cambridge");
        zipNeighborhoodMap.put("02445",	"Brookline");
        zipNeighborhoodMap.put("02446",	"Brookline");
        zipNeighborhoodMap.put("02447",	"Brookline Village");
    }

    public String getNeighborhoodByZipCode(String zipcode, String altNeighborhood) {
        if (this.zipNeighborhoodMap.containsKey(zipcode)) {
            return this.zipNeighborhoodMap.get(zipcode);
        } else {
            return altNeighborhood;
        }
    }
}
