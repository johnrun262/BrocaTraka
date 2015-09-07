package com.john.brocatraka.main;

import android.location.Location;

/**
 * Created by john on 9/4/15.
 */
public class TrackedObjectsConnector {

    private final TrackerAppActivity activity;

    public TrackedObjectsConnector(TrackerAppActivity activity) {
        this.activity = activity;
    }

    public void onCreate() {
        CoffeeTree.onCreate(activity);
    }

    public void stopInput() {
        CoffeeTree.stopInput(activity);
    }

    public void newButton() {
        CoffeeTree.newButton(activity);
    }

    public void saveButton(Location location, int minSat, float minSNR, float maxAccuracy,
                           int numFixes, String userName) throws Exception {
        CoffeeTree.saveButton(activity, location, minSat, minSNR, maxAccuracy, numFixes, userName);
    }

    public void clearButton() {
        CoffeeTree.clearButton(activity);
    }

    public int getNumObjects() {
        return (CoffeeTree.getNumObjects());
    }

}
