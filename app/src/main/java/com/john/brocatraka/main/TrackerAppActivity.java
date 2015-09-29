package com.john.brocatraka.main;

//
// John Lloyd
// July 2012 - June 2015
//
// This source code was developed Summer 2012 by John Lloyd to use a cellular phone to track
// spatial features and display the results on a map such as those provided by Google. The idea
// to use a cellular phone to track broca on a coffee farm was presented to Pamela Salazar
// March 30, 2015 by John Lloyd, which initiated discussions with Hacienda Venecia. The
// display of the source code developed in Summer 2012 was customize during April and May
// 2015 by John Lloyd to allow input of broca numbers. John Lloyd arrived at Hacienda Venecia
// May 26 and a successful field trial was conducted the same day. John Lloyd spend 12 days
// during May and June 2015 at Hacienda Venecia conducting field trials and collecting feedback.
//

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class TrackerAppActivity extends Activity {

    private LocationManager locationManager; // used to request location updated and get gps status
    private TextView gpsStatusView;          // used to update UI with status

    private static TrackedObjectsConnector trackedObjectsConnector;

    private int gpsMinSat = 0;               // the minimum number of satellites that must be used for fix
    private float gpsMinSNR = 0;             // the minimum signal to noise ratio before we can use a satellite
    private float gpsMaxAccuracy = 100;      // the maximum accuracy of a location before we will use it
    private int gpsNumberWithMinSNR = 0;     // the number of satellites meeting SNR minimum

    //private static Mutex gpsMinsMutex;       // to lock update/use of gpsNumberWithMinSNR since accessed from three threads
    private ArrayList<GpsSatellite> gpsSatelliteList; // to loop through satellites to get status
    private int gpsNumFixes = 0;             // Number of locations used in the average
    private float gpsLastAccuracy = 0;       // the accuracy value of the last GPS location
    private float gpsLastBestSNR = 0;        // the last best (greatest) signal to noise ratio of satellites
    private Location gpsAvgLocation;         // location calculated as a result of position averaging
    private ArrayList<Location> gpsLocations; // to loop through satellites to get status

    private enum ProviderType {GPS, CELL}

    ;   // Used in location listener to determine what provider it was registered for

    private enum ButtonType {NEW_TREE, SAVE_TREE, CANCEL_TREE, EMAIL, MAP, CLEAR, NEW_FILE}

    ; // used in the onClickListener to tell what was clicked

    private enum NewButtonFunction {NEW, CANCEL}

    ; // new button toggles function
    private NewButtonFunction currNewButtonFunction = NewButtonFunction.NEW;
    View newButton;
    View saveButton;

    private MyLocationListener gpslocationListener; // listener for GPS location updates - added/removed in Start/Stop button

    // file which data is written to
    final private static String FILE_NAME = "trackerDataFile";
    final private static String FILE_TYPE = ".txt";
    final private static String USER_NAME = "userNameFile";
    final private static String DIR_NAME = "gpsData";

    // name of the user
    private String userName = "";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        AlertDialog.Builder alertDialogBuilder; // Used to build confirm and text input popups
        AlertDialog alertDialog;                // Used to build confirm and text input popups

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brocatraka);

        // get the UI object that I will update with status of the gps
        gpsStatusView = (TextView) findViewById(R.id.gpsStatus);

        // get the location manager object to use when adding location listeners and getting gps status
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // LISTENERS

        // add a listener for gps status updates
        MyGpsStatusListener gpsStatusListener = new MyGpsStatusListener();
        locationManager.addGpsStatusListener(gpsStatusListener);

        // add a listener for location updates from the cell network
        try {
            MyLocationListener networkLocationListener = new MyLocationListener((TextView) findViewById(R.id.networkLongLat), ProviderType.CELL);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkLocationListener);
        } catch (Exception ex) {
            Toast.makeText(TrackerAppActivity.this, getString(R.string.noCellularProvider), Toast.LENGTH_SHORT).show();
        }

        // add a listener for location updates from the gps
        gpslocationListener = new MyLocationListener((TextView) findViewById(R.id.gpsLongLat), ProviderType.GPS);
        // listener for location updates from gps added in Start Button listener and removed in Stop Button listener
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpslocationListener);

        // Setup Generic UI elements

        // BUTTONS

        // Setup listener for new tree button clicks
        newButton = findViewById(R.id.newTree);
        newButton.setOnClickListener(new MyOnClickListener(ButtonType.NEW_TREE));

        // Setup listener for save tree button clicks
        saveButton = findViewById(R.id.saveTree);
        saveButton.setOnClickListener(new MyOnClickListener(ButtonType.SAVE_TREE));

        // Setup listener for cancel tree button clicks
        //View cancelButton = findViewById(R.id.cancelTree);
        //cancelButton.setOnClickListener(new MyOnClickListener(ButtonType.CANCEL_TREE));

        // Setup listener for email button clicks
        //View newFileButton = findViewById(R.id.gpsNewFile);
        //newFileButton.setOnClickListener(new MyOnClickListener(ButtonType.NEW_FILE));

        // Setup listener for email button clicks
        View emailButton = findViewById(R.id.gpsEmail);
        emailButton.setOnClickListener(new MyOnClickListener(ButtonType.EMAIL));

        // Setup listener for clear button clicks
        View clearButton = findViewById(R.id.gpsClear);
        clearButton.setOnClickListener(new MyOnClickListener(ButtonType.CLEAR));

        // Get a TrackedObjectsConnector
        trackedObjectsConnector = new TrackedObjectsConnector(this);
        trackedObjectsConnector.onCreate();

        // stop input until new tree pushed
        trackedObjectsConnector.stopInput();

        // grey out save button
        saveButton.setEnabled(false);

        // set the new button function to New
        currNewButtonFunction = NewButtonFunction.NEW;
        ((Button) newButton).setText(getString(R.string.newTree));

        // get the name of the user
        promptForUserName();

        //  turn on tracking only mode if confirmed

        /* TODO alertDialogBuilder = new AlertDialog.Builder(TrackerAppActivity.this);
        alertDialogBuilder.setTitle(getString(R.string.confirmTrackingMode));
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                trackingMode = true;
            }
        });
        alertDialogBuilder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                trackingMode = false;
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();*/
    }

    private void promptForUserName () {
        AlertDialog.Builder alert = new AlertDialog.Builder(
                new ContextThemeWrapper(this, android.R.style.Theme_Dialog));

        alert.setTitle(getString(R.string.userNamePromptTitle));
        alert.setMessage("");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setTextAppearance(this, R.style.edit_text);
        input.setText(readUserName());
        alert.setView(input);

        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Editable inputText = input.getText();
                userName = inputText.toString();
                userName = userName.replace(',', '.');
                writeUserName(userName);
            }
        });

        alert.show();

    }

    // Class to handle button clicks
    private class MyOnClickListener implements View.OnClickListener {
        private ButtonType buttonType;        // Constructor sets the button that the instance associated with

        public MyOnClickListener(ButtonType type) {
            buttonType = type;
        }

        public void onClick(View v) {
            AlertDialog.Builder alertDialogBuilder; // Used to build confirm and text input popups
            AlertDialog alertDialog;                // Used to build confirm and text input popups

            // this is ugly but allows swapping of function on new/cancel button
            ButtonType switchButtonType = buttonType;

            if (switchButtonType == ButtonType.NEW_TREE) {
                if (currNewButtonFunction == NewButtonFunction.CANCEL) {
                    switchButtonType = ButtonType.CANCEL_TREE;
                }
            }

            switch (switchButtonType) {
                case NEW_TREE:

                    // set number of locations received from GPS listener to zero before adding the listener
                    gpsNumFixes = 0;
                    // also clear previous average location
                    gpsAvgLocation = null;
                    // add a listener for location updates from the gps
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpslocationListener);

                    // ungrey out save button
                    saveButton.setEnabled(true);

                    // set the new button function to Cancel
                    currNewButtonFunction = NewButtonFunction.CANCEL;
                    ((Button) newButton).setText(getString(R.string.cancelTree));

                    // Clear the UI values and allow input
                    trackedObjectsConnector.newButton();

                    break;

                case SAVE_TREE:

                    try {

                        if (gpsAvgLocation == null) {
                            Toast.makeText(TrackerAppActivity.this, getString(R.string.noLocationToSave), Toast.LENGTH_LONG).show();
                            return;
                        }

                        trackedObjectsConnector.saveButton(getAvgOfBestLocations(), gpsMinSat,
                                gpsMinSNR, gpsMaxAccuracy, gpsNumFixes, userName);

                    } catch (Exception e) {
                        Toast.makeText(TrackerAppActivity.this, getString(R.string.saveFailed), Toast.LENGTH_LONG).show();
                    }

                    // no break so fall through into cancel

                case CANCEL_TREE:

                    // unregister listener to stop updates (and save battery?)
                    locationManager.removeUpdates(gpslocationListener);

                    // set number of locations received from GPS listener to zero
                    gpsNumFixes = 0;
                    // also clear previous average location
                    gpsAvgLocation = null;

                    // stop additional input until new tree pushed
                    trackedObjectsConnector.stopInput();

                    // grey out save button
                    saveButton.setEnabled(false);

                    // set the new button function to New
                    currNewButtonFunction = NewButtonFunction.NEW;
                    ((Button) newButton).setText(getString(R.string.newTree));

                    break;

                case NEW_FILE:
                    break;

                case EMAIL:

                    // any data to send?
                    if (trackedObjectsConnector.getNumObjects() == 0) {
                        Toast.makeText(TrackerAppActivity.this, getString(R.string.noDataToSend), Toast.LENGTH_LONG).show();
                        break;
                    }

                    // create an email and send
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.emailSubject));

                    // add attachment with data
                    try {
                        File file = new File(getWorkingFileName());

                        // copy file before attaching to create a snapshot to handle offline case
                        File toFile = new File(getUniqueFileName());
                        copyFile(file, toFile);

                        Uri uri = Uri.fromFile(toFile);
                        Toast.makeText(TrackerAppActivity.this, "Attachment URI: " + uri.toString(), Toast.LENGTH_LONG).show();
                        intent.putExtra(Intent.EXTRA_TEXT, uri);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);

                    } catch (Exception e) {
                        Toast.makeText(TrackerAppActivity.this, getString(R.string.couldNotAddAttachment), Toast.LENGTH_LONG).show();
                    }

                    // try to send
                    try {
                        startActivity(Intent.createChooser(intent, getString(R.string.sendingEmail)));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(TrackerAppActivity.this, getString(R.string.noEmailClients), Toast.LENGTH_LONG).show();
                    }

                    break;

                case CLEAR:

                    // show a confirmation dialog to confirm the user really want to clear list
                    alertDialogBuilder = new AlertDialog.Builder(TrackerAppActivity.this);
                    alertDialogBuilder.setTitle(getString(R.string.confirmClearing) + " " +
                            String.valueOf(trackedObjectsConnector.getNumObjects()) + " " + getString(R.string.trackedObject) + "?");
                    alertDialogBuilder.setCancelable(true);
                    alertDialogBuilder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // finally clear the list if the user clicks yes
                            trackedObjectsConnector.clearButton();
                        }
                    });
                    alertDialogBuilder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

                    alertDialog = alertDialogBuilder.create();
                    alertDialog.show();

                    break;

            }
        }
    }

    // Method to update the UI GPS status field
    private void gpsUpateStatusField() {
        String displayString;
        displayString = //" Sats: " + String.valueOf(gpsNumberWithMinSNR) +
                " Acc: " + String.valueOf(gpsLastAccuracy) + "m" +
                        //" SNR: " + String.valueOf(gpsLastBestSNR) +
                        " #Fix: " + String.valueOf(gpsNumFixes);
        gpsStatusView.setText(displayString);
    }

    // Method to return number of satellites with minimum SNR when GPS status changes or UI updates made
    private int getGPSNumberWithMinSNR() {
        // count the number of satellites used in fix that meet the minimum signal to noise ration
        int gpsNumberWithMinSNR = 0;
        if (gpsSatelliteList != null) {
            for (GpsSatellite s : gpsSatelliteList) {
                // was this GPS used in fix and does its SNR meet the minimum?
                if ((s.usedInFix()) && (s.getSnr() > gpsMinSNR)) {
                    gpsNumberWithMinSNR = gpsNumberWithMinSNR + 1;
                }
            }
        } else {
            // if we got a fix with no satellites assume we are in the emulator and all meet SNR
            gpsNumberWithMinSNR = 99;
        }
        return gpsNumberWithMinSNR;
    }

    // Methods in this class are called when the status of the GPS changes
    private class MyGpsStatusListener implements GpsStatus.Listener {
        // called to handle an event updating the satellite status
        private void satelliteStatusUpdate() {
            // use the location manager to get a gps status object
            // this method should only be called inside GpsStatus.Listener
            GpsStatus gpsStatus = locationManager.getGpsStatus(null);

            // create an iterator to loop through list of satellites
            Iterable<GpsSatellite> iSatellites = gpsStatus.getSatellites();
            Iterator<GpsSatellite> gpsSatelliteIterator = iSatellites.iterator();

            // lock this region since gpsNumberWithSNR also updated and
            // gpsSatelliteList accessed on UI thread
            //gpsNumberMutex.lock();

            // find the satellite with the best (greatest signal to noise ratio to update display
            // and save list of satellites in an ArrayList
            gpsLastBestSNR = 0;
            gpsSatelliteList = new ArrayList<GpsSatellite>();
            while (gpsSatelliteIterator.hasNext()) {
                // get next satellite from iterator
                GpsSatellite s = (GpsSatellite) gpsSatelliteIterator.next();
                // and add to ArrayList
                gpsSatelliteList.add(s);
                // is the SNR of this satellite the best (greatest)?
                if (gpsLastBestSNR < s.getSnr()) {
                    gpsLastBestSNR = s.getSnr();
                }
            }

            // call method to loop through list and return number with minimum signal to noise ration
            gpsNumberWithMinSNR = getGPSNumberWithMinSNR();

            // update the status field on UI with new number meeting minimum SNR and best SNR
            gpsUpateStatusField();

            //gpsNumberMutex.unlock();
        }

        // the status of the GPS has changed
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    satelliteStatusUpdate();
                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    break;
            }
        }
    }

    ;

    // Methods in this class are called when the location providers give an update
    private class MyLocationListener implements LocationListener {
        private TextView locationView;        // UI object that contains enabled, disabled, or location string
        private ProviderType providerType;    // Constructor sets the provider type (i.e. network or gps)

        // constructor takes a UI textview object so I know where to place location
        // and provider type object so I know to do averaging if GPS
        public MyLocationListener(TextView view, ProviderType type) {
            locationView = view;
            providerType = type;
        }

        // update the location on the UI at the specified textview object
        private void updateUILocationField(Location location) {
            if (location == null) {
                // should never happen
                locationView.setText("bad location");
                return;
            }

            String longString = String.format("%10.6f", Math.abs(location.getLongitude()));
            String latString = String.format("%10.6f", Math.abs(location.getLatitude()));
            String altString = String.format("%6.1f", Math.abs(location.getAltitude()));
            String accuracyString = String.format("%5.1f", Math.abs(location.getAccuracy()));

            String locationString = longString + ((location.getLongitude()) < 0 ? " W " : " E ") +
                    latString + ((location.getLatitude()) < 0 ? " S " : " N ") +
                    altString + "m " +
                    accuracyString + "m";

            locationView.setText(locationString);
        }

        // the provider has updated the location
        public void onLocationChanged(Location location) {

            // if the location came from GPS
            if (providerType == ProviderType.GPS) {
                // lock this region since gpsNumberWithSNR updated on gpsStatus and UI threads
                //gpsNumberMutex.lock();

                // do I have enough satellites used in the fix and
                // does the accuracy meet minimum?
                if ((gpsNumberWithMinSNR >= gpsMinSat) && (location.getAccuracy() <= gpsMaxAccuracy)) {
                    // is this the first fix?
                    if (gpsNumFixes == 0) {
                        // set the average to the first location
                        gpsAvgLocation = new Location(location);
                        gpsLocations = new ArrayList<Location>();
                    } else {
                        // not the first location so update averages for longitude, latitude and altitude
                        gpsAvgLocation.setLongitude(
                                (gpsAvgLocation.getLongitude() * gpsNumFixes + location.getLongitude()) / (gpsNumFixes + 1));
                        gpsAvgLocation.setLatitude(
                                (gpsAvgLocation.getLatitude() * gpsNumFixes + location.getLatitude()) / (gpsNumFixes + 1));
                        gpsAvgLocation.setAltitude(
                                (gpsAvgLocation.getAltitude() * gpsNumFixes + location.getAltitude()) / (gpsNumFixes + 1));
                        gpsAvgLocation.setAccuracy(
                                (gpsAvgLocation.getAccuracy() * gpsNumFixes + location.getAccuracy()) / (gpsNumFixes + 1));
                    }
                    // update the average location on the UI
                    updateUILocationField(gpsAvgLocation);
                    // increment the number of fixes
                    gpsNumFixes = gpsNumFixes + 1;
                    // add location to array of locations
                    gpsLocations.add(location);
                }

                // update status to show accuracy of this location
                gpsLastAccuracy = location.getAccuracy();
                gpsUpateStatusField();

                // gpsNumberMutex.unlock();
            } else {
                // not GPS location so use as-is without averaging
                // but should update the network location field
                updateUILocationField(location);
            }

        }

        // do nothing when status changed
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        // handled in GPS status listener
        public void onProviderEnabled(String provider) {
        }

        // handled in GPS status listener
        public void onProviderDisabled(String provider) {
        }

    }

    private Location getAvgOfBestLocations() {
        float bestAccuracy = 100;
        Location avgLocation;
        int numFixes;

        // find the best accuracy in the set of all locations
        for (Location location : gpsLocations) {
            if (location.getAccuracy() < bestAccuracy) {
                bestAccuracy = location.getAccuracy();
            }
        }

        // use the locations with the best accuracy to find an average
        avgLocation = null;
        numFixes = 0;

        for (Location location : gpsLocations) {
            if (location.getAccuracy() <= bestAccuracy) {
                if (numFixes == 0) {
                    avgLocation = new Location(location);
                } else {
                    // not the first location so update averages for longitude, latitude and altitude
                    avgLocation.setLongitude(
                            (avgLocation.getLongitude() * numFixes + location.getLongitude()) / (numFixes + 1));
                    avgLocation.setLatitude(
                            (avgLocation.getLatitude() * numFixes + location.getLatitude()) / (numFixes + 1));
                    avgLocation.setAltitude(
                            (avgLocation.getAltitude() * numFixes + location.getAltitude()) / (numFixes + 1));
                    avgLocation.setAccuracy(
                            (avgLocation.getAccuracy() * numFixes + location.getAccuracy()) / (numFixes + 1));
                }
                numFixes++;
            }
        }

        return avgLocation;

    }

    // append the data collected to a file
    public boolean writeToFile(String data, Boolean append, String heading) {

        try {

            final File dir = new File(getOutFileDir(), DIR_NAME);
            dir.mkdirs();


            File file = new File(getWorkingFileName());

            // if we are appending to a file and it doesn't exist then write the heading first
            if (append && !file.exists()) {
                data = heading + "\n" + data;
            } else {
                if (!append) {
                    // rename existing file and keep 2 weeks worth of data

                    // for safety delete just renames the file
                    File toFile = new File(getUniqueFileName());
                    Boolean success = file.renameTo(toFile);

                    // delete those older than limit
                    Calendar c = Calendar.getInstance();
                    long nowMills = c.getTimeInMillis();
                    File[] fileList = dir.listFiles();
                    for (File f : fileList) {
                        String fileName = f.getName();
                        if (fileName.startsWith(FILE_NAME)) {
                            Date lastModified = new Date(f.lastModified());
                            long diff = nowMills - lastModified.getTime();
                            long diffDays = TimeUnit.MILLISECONDS.toDays(diff);
                            if (diffDays > 14) {
                                f.delete();
                            }
                        }
                    }

                    // now create a new file to work with
                    file = new File(getWorkingFileName());
                    file.createNewFile();
                }
            }

            Writer out = new BufferedWriter(new FileWriter(file, append));
            out.write(data + "\n");
            out.flush();
            out.close();

            MediaScannerHelper msh = new MediaScannerHelper();
            msh.addFile(file.getAbsolutePath());

        } catch (Exception e) {
            Toast.makeText(TrackerAppActivity.this, getString(R.string.exceptionWritingToFile), Toast.LENGTH_LONG).show();
            return false;
        }

        return true;

    }

    private void writeUserName(String userName) {

        try {

            final File dir = new File(getOutFileDir(), DIR_NAME);
            dir.mkdirs();

            File file = new File(getUserFileName());
            Writer out = new BufferedWriter(new FileWriter(file, false));
            out.write(userName + "\n");
            out.flush();
            out.close();

        } catch (Exception e) {
            Toast.makeText(TrackerAppActivity.this, getString(R.string.exceptionWritingToFile), Toast.LENGTH_LONG).show();
        }

    }

    private String readUserName() {

        try {

            File file = new File(getUserFileName());
            BufferedReader in = new BufferedReader(new FileReader(file));
            String inputString = in.readLine();

            if (inputString == null) {
                return "";
            }

            return (inputString);

        } catch (Exception e) {
            Toast.makeText(TrackerAppActivity.this, getString(R.string.exceptionReadingFromFile), Toast.LENGTH_LONG).show();
        }

        return "";

    }

    private File getOutFileDir() {
        return getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    private String getUserFileName() {
        return (getOutFileDir() + "/" + DIR_NAME + "/" + USER_NAME + FILE_TYPE);
    }

    private String getWorkingFileName() {
        return (getOutFileDir() + "/" + DIR_NAME + "/" + FILE_NAME + FILE_TYPE);
    }

    private String getUniqueFileName() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        return (getOutFileDir() + "/" + DIR_NAME + "/" +  FILE_NAME + df.format(c.getTime()) + FILE_TYPE);
    }

    // to make file show up in Windows Explorer
    private class MediaScannerHelper implements MediaScannerConnection.MediaScannerConnectionClient {

        public void addFile(String filename) {
            String[] paths = new String[1];
            paths[0] = filename;
            MediaScannerConnection.scanFile(getApplicationContext(), paths, null, this);
        }

        public void onMediaScannerConnected() {
        }

        public void onScanCompleted(String path, Uri uri) {
        }
    }

    public ArrayList<String> readFile() {

        ArrayList<String> rows = new ArrayList<String>();

        try {

            String inputString;

            File file = new File(getWorkingFileName());
            BufferedReader in = new BufferedReader(new FileReader(file));

            while ((inputString = in.readLine()) != null) {
                rows.add(inputString);
            }

        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.exceptionReadingFromFile), Toast.LENGTH_LONG).show();
        }

        return rows;

    }

    public void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

}
