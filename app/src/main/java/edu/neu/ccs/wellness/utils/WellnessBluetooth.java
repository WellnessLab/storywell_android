package edu.neu.ccs.wellness.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

/**
 * Created by hermansaksono on 3/19/19.
 */

public class WellnessBluetooth {

    public static final int PERMISSION_REQUEST_LOCATION = 8100;
    public static String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    public static String[] COARSE_PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    /**
     * Try show coarse and fine location permission request if no permission was given yet.
     * @param activity
     */
    public static void tryRequestLocationPermission(Activity activity) {
        if (!isLocationPermissionAllowed(activity)) {
            ActivityCompat.requestPermissions(
                    activity, LOCATION_PERMISSIONS, PERMISSION_REQUEST_LOCATION);
        }
    }

    /**
     * Determines if coarse and fine location permission has been given by the user.
     * @param context
     * @return
     */
    public static boolean isLocationPermissionAllowed(Context context) {
        int permissionCoarseLocation = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionFineLocation = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionCoarseLocation == PackageManager.PERMISSION_GRANTED
                && permissionFineLocation == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Try show coarse location permission request if no permission was given yet.
     * @param activity
     */
    public static void tryRequestCoarsePermission(Activity activity) {
        if (!isCoarseLocationAllowed(activity)) {
            ActivityCompat.requestPermissions(activity, COARSE_PERMISSIONS,
                    PERMISSION_REQUEST_LOCATION);
        }
    }

    /**
     * Determines if coarse location permission has been given by the user.
     * @param context
     * @return
     */
    public static boolean isCoarseLocationAllowed(Context context) {
        int permissionCoarseLocation = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionCoarseLocation == PackageManager.PERMISSION_GRANTED;
    }
}
