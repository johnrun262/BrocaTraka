package com.john.brocatraka.main;

import android.location.Location;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by john on 9/4/15.
 */
public class CoffeeTree {

    private static EditText numInfectedView;        // used to get/set Number infected on UI
    private static EditText totalNumView;           // used to get/set Total Number on UI
    private static EditText lotNumView;             // used to get/set Lot Number on UI
    private static EditText bagNumView;             // used to get/set Bag Number on UI
    private static EditText descriptionView;        // used to get/set Description on UI

    private static Spinner treeSpinner;             // UI object presenting tree spinner
    private static Spinner branchSpinner;           // UI object presenting branch location spinner
    private static Spinner stateSpinner;            // UI object presenting state location spinner

    private static ArrayList<TreeObject> treeList = new ArrayList<TreeObject>();
    ; // List of all saved vertices

    // the last lot and bag to populate UI automagically
    private static int lastLotNum = 0;
    private static int lastBagNum = 0;

    private static class TreeObject {
        private String desc;         // text description entered on UI
        private String branchPos;    // relative position of the branch on the tree
        private String brocaState;   // the state of the broca
        private int numInfected;     // the number of infected beans entered by user
        private int totalNum;        // the total number of beans entered by the user
        private int percentInfected; // the percentage of bean infected
        private int lotNum;          // lot number
        private int bagNum;          // bag number
        private double longitude;    // location
        private double latitude;     // location
        private double altitude;     // location
        private float accuracy;      // location
        private int minSat;          // the minimum number of satellites required while averaging
        private float minSNR;        // the minimum SNR required while averaging
        private float maxAccuracy;   // the maximum accuracy required while averaging
        private int numFixes;        // the total number of fixes used to get this vertex
        private String date;         // the date the data was captured
        private String userName;     // the name of the user that captured the data

        // constructor to set attributes for the vertex
        public TreeObject(String desc, String branchPos, String brocaState, int numInfected, int totalNum, int lotNum, int bagNum,
                          Location location, int minSat, float minSNR, float maxAccuracy,
                          int numFixes, String userName) throws Exception {

            this.desc = desc;
            this.branchPos = branchPos;
            this.brocaState = brocaState;
            this.numInfected = numInfected;
            this.totalNum = totalNum;

            if (this.totalNum != 0) {
                this.percentInfected = 100 * this.numInfected / this.totalNum;
            } else {
                this.percentInfected = 0;
            }

            this.lotNum = lotNum;
            this.bagNum = bagNum;

            this.longitude = location.getLongitude();
            this.latitude = location.getLatitude();
            this.altitude = location.getAltitude();
            this.accuracy = location.getAccuracy();
            this.minSat = minSat;
            this.minSNR = minSNR;
            this.maxAccuracy = maxAccuracy;
            this.numFixes = numFixes;

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            this.date = df.format(c.getTime());

            this.userName = userName;
        }

        // tree object from CSV file values
        public TreeObject(String[] rowData) throws Exception {
            int rowPos = 0;
            this.desc = rowData[rowPos++];
            this.branchPos = rowData[rowPos++];
            this.brocaState = rowData[rowPos++];
            this.longitude = Double.valueOf(rowData[rowPos++]);
            this.latitude = Double.valueOf(rowData[rowPos++]);
            this.altitude = Double.valueOf(rowData[rowPos++]);
            this.accuracy = Float.valueOf(rowData[rowPos++]);
            this.numInfected = Integer.valueOf(rowData[rowPos++]);
            this.totalNum = Integer.valueOf(rowData[rowPos++]);
            this.percentInfected = Integer.valueOf(rowData[rowPos++]);
            this.lotNum = Integer.valueOf(rowData[rowPos++]);
            this.bagNum = Integer.valueOf(rowData[rowPos++]);
            this.numFixes = Integer.valueOf(rowData[rowPos++]);
            this.date = rowData[rowPos++];
            this.userName = rowData[rowPos++];

            lastLotNum = this.lotNum;
            lastBagNum = this.bagNum;
        }

        // CSV String format for output
        public String toString() {
            try {
                String outString = this.desc + ",";

                outString = outString + this.branchPos + ",";

                outString = outString + this.brocaState + ",";

                outString = outString +
                        String.valueOf(this.longitude) + "," +
                        String.valueOf(this.latitude) + "," +
                        String.valueOf(this.altitude) + "," +
                        String.valueOf(this.accuracy) + ",";


                outString = outString +
                        String.valueOf(this.numInfected) + "," +
                        String.valueOf(this.totalNum) + ",";

                outString = outString +
                        String.valueOf(this.percentInfected) + ",";

                outString = outString +
                        String.valueOf(this.lotNum) + "," +
                        String.valueOf(this.bagNum) + ",";

                outString = outString +
                        String.valueOf(this.numFixes) + ",";

                outString = outString + this.date + ",";

                outString = outString + this.userName;

                return (outString);

            } catch (Exception e) {
                return "";
            }
        }
    }

    public static void onCreate(TrackerAppActivity activity) {

        // Setup UI Objects

        // SPINNERS

        treeSpinner = (Spinner) activity.findViewById(R.id.treeSpinner);
        ArrayAdapter<CharSequence> treeSpinnerAdapter =
                ArrayAdapter.createFromResource(activity, R.array.treeSpinnerEmpty, android.R.layout.simple_spinner_item);
        treeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        treeSpinner.setAdapter(treeSpinnerAdapter);
        treeSpinner.setOnItemSelectedListener(new treeSpinnerListener());

        branchSpinner = (Spinner) activity.findViewById(R.id.branchSpinner);
        ArrayAdapter<CharSequence> branchSpinnerAdapter =
                ArrayAdapter.createFromResource(activity, R.array.branchSpinner, android.R.layout.simple_spinner_item);
        branchSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        branchSpinner.setAdapter(branchSpinnerAdapter);
        branchSpinner.setOnItemSelectedListener(new branchSpinnerListener());

        stateSpinner = (Spinner) activity.findViewById(R.id.stateSpinner);
        ArrayAdapter<CharSequence> stateSpinnerAdapter =
                ArrayAdapter.createFromResource(activity, R.array.stateSpinner, android.R.layout.simple_spinner_item);
        stateSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        stateSpinner.setAdapter(stateSpinnerAdapter);
        stateSpinner.setOnItemSelectedListener(new stateSpinnerListener());

        // populate spinner with data in file if exists
        readTreesFromFile(activity);
        populateTreeSpinner(activity);

        // INPUT FIELDS

        numInfectedView = (EditText) activity.findViewById(R.id.numInfected);
        totalNumView = (EditText) activity.findViewById(R.id.totalNum);
        lotNumView = (EditText) activity.findViewById(R.id.lotNum);
        bagNumView = (EditText) activity.findViewById(R.id.bagNum);
        descriptionView = (EditText) activity.findViewById(R.id.description);


    }

    // Methods in this class are called when the user interacts with the tree spinner on the UI
    private static class treeSpinnerListener implements AdapterView.OnItemSelectedListener {
        // the user selected one of the items on one of the spinners
        public void onItemSelected(AdapterView<?> parent, View view, int pos,
                                   long arg3) {
        }

        // nothing selected do nothing
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    // Methods in this class are called when the user interacts with the branch spinner on the UI
    private static class branchSpinnerListener implements AdapterView.OnItemSelectedListener {
        // the user selected one of the items on one of the spinners
        public void onItemSelected(AdapterView<?> parent, View view, int pos,
                                   long arg3) {
        }

        // nothing selected do nothing
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    // Methods in this class are called when the user interacts with the broca state spinner on the UI
    private static class stateSpinnerListener implements AdapterView.OnItemSelectedListener {
        // the user selected one of the items on one of the spinners
        public void onItemSelected(AdapterView<?> parent, View view, int pos,
                                   long arg3) {
        }

        // nothing selected do nothing
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    private static void readTreesFromFile(TrackerAppActivity activity) {

        ArrayList<String> rows = activity.readFile();

        for (String row : rows) {
            String[] rowData = row.split(",");
            try {
                TreeObject tree = new TreeObject(rowData);
                treeList.add(tree);
            } catch (Exception e) {

            }
        }

    }

    private static void populateTreeSpinner(TrackerAppActivity activity) {

        ArrayList<String> treeStrings = new ArrayList<String>();

        if (treeList.size() == 0) {
            treeStrings = new ArrayList<String>(Arrays.asList(activity.getResources().getStringArray(R.array.treeSpinnerEmpty)));
        } else {
            int i = treeList.size();
            while (i > 0) {
                i--;
                TreeObject tree = treeList.get(i);
                String desc = tree.desc;
                if (desc.length() > 30) {
                    desc = desc.substring(0, 30);
                }

                String s = String.valueOf(tree.lotNum) + "/" + String.valueOf(tree.bagNum) + " " + desc +
                        " " + String.valueOf(tree.percentInfected) + "%(" +
                        String.valueOf(tree.numInfected) + "/" + String.valueOf(tree.totalNum) +
                        ")";

                treeStrings.add(s);
            }
        }

        ArrayAdapter<String> treeSpinnerAdapter =
                new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, treeStrings);
        treeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        treeSpinner.setAdapter(treeSpinnerAdapter);

    }

    private static void allowInput(TrackerAppActivity activity) {
        numInfectedView.setInputType(InputType.TYPE_CLASS_NUMBER);
        numInfectedView.setEnabled(true);
        numInfectedView.getText().clear();
        totalNumView.setInputType(InputType.TYPE_CLASS_NUMBER);
        totalNumView.setEnabled(true);
        totalNumView.getText().clear();
        lotNumView.setInputType(InputType.TYPE_CLASS_NUMBER);
        lotNumView.setEnabled(true);
        lotNumView.getText().clear();
        bagNumView.setInputType(InputType.TYPE_CLASS_NUMBER);
        bagNumView.setEnabled(true);
        bagNumView.getText().clear();
        descriptionView.setInputType(InputType.TYPE_CLASS_TEXT);
        descriptionView.setEnabled(true);
        descriptionView.getText().clear();
        branchSpinner.setEnabled(true);
        branchSpinner.setSelection(1);
        stateSpinner.setEnabled(true);
        stateSpinner.setSelection(0);

    }

    public static void stopInput(TrackerAppActivity activity) {
        numInfectedView.getText().clear();
        numInfectedView.setInputType(InputType.TYPE_NULL);
        numInfectedView.setEnabled(false);
        totalNumView.getText().clear();
        totalNumView.setInputType(InputType.TYPE_NULL);
        totalNumView.setEnabled(false);
        lotNumView.getText().clear();
        lotNumView.setInputType(InputType.TYPE_NULL);
        lotNumView.setEnabled(false);
        bagNumView.getText().clear();
        bagNumView.setInputType(InputType.TYPE_NULL);
        bagNumView.setEnabled(false);
        descriptionView.getText().clear();
        descriptionView.setInputType(InputType.TYPE_NULL);
        descriptionView.setEnabled(false);
        branchSpinner.setEnabled(false);
        branchSpinner.setSelection(1);
        stateSpinner.setEnabled(false);
        stateSpinner.setSelection(0);

    }

    // CSV Heading String
    private static String getHeading(TrackerAppActivity activity) {

        String outString = activity.getString(R.string.descriptionPrompt) + ",";

        outString = outString + activity.getString(R.string.branchPositionHeading) + ",";

        outString = outString + activity.getString(R.string.brocaStateHeading) + ",";

        outString = outString +
                "Longitude" + "," +
                "Latitude" + "," +
                "Altitude" + "," +
                "Accuracy" + ",";

        outString = outString +
                activity.getString(R.string.numInfectedPrompt) + "," +
                activity.getString(R.string.totalNumPrompt) + ",";


        outString = outString +
                activity.getString(R.string.percentageInfectedHeading) + ",";

        outString = outString +
                activity.getString(R.string.lotNumPrompt) + ",";

        outString = outString +
                activity.getString(R.string.bagNumPrompt) + ",";

        outString = outString +
                "Number of Fixes" + ",";

        outString = outString +
                activity.getString(R.string.dateHeading) + ",";

        outString = outString +
                activity.getString(R.string.userNameHeading);

        return (outString);
    }

    public static void newButton(TrackerAppActivity activity) {
        allowInput(activity);

        // use the lot and bag number from the last tree if there is one
        totalNumView.requestFocus();

        bagNumView.setText(Integer.toString(lastBagNum + 1));
        lotNumView.setText(Integer.toString(lastLotNum));

    }

    public static void saveButton(TrackerAppActivity activity, Location location, int minSat, float minSNR, float maxAccuracy,
                                  int numFixes, String userName) throws Exception {
        int numInfected = 0;
        int totalNum = 0;

        try {
            totalNum = Integer.valueOf(totalNumView.getText().toString());
            numInfected = Integer.valueOf(numInfectedView.getText().toString());

        } catch (Exception e) {
            Toast.makeText(activity, activity.getString(R.string.numInfectedTotalRequired), Toast.LENGTH_LONG).show();
            throw new Exception();
        }

        if (numInfected > totalNum) {
            Toast.makeText(activity, activity.getString(R.string.numInfectedGreaterThanTotal), Toast.LENGTH_LONG).show();
            throw new Exception();
        }

        int lotNum;
        int bagNum;

        try {
            lotNum = Integer.valueOf(lotNumView.getText().toString());
            bagNum = Integer.valueOf(bagNumView.getText().toString());
        } catch (Exception e) {
            lotNum = 0;
            bagNum = 0;
            throw new Exception();
        }

        // to populate next tree
        lastLotNum = lotNum;
        lastBagNum = bagNum;

        // get the branch location from the spinner
        String branchPos = (String) branchSpinner.getSelectedItem();

        // get the broca state from the spinner
        String brocaState = (String) stateSpinner.getSelectedItem();

        // get the description and remove commas
        String desc = descriptionView.getText().toString();
        desc = desc.replace(',', '.');

        // create a tree object to save the averaged location
        TreeObject tree = new TreeObject(desc.toString(),
                branchPos, brocaState, numInfected, totalNum, lotNum, bagNum,
                location, minSat,
                minSNR, maxAccuracy, numFixes, userName);

        // add to list of tree
        treeList.add(tree);
        populateTreeSpinner(activity);
        activity.writeToFile(tree.toString(), true, getHeading(activity));

        Toast.makeText(activity, activity.getString(R.string.saved), Toast.LENGTH_LONG).show();

    }

    public static void clearButton(TrackerAppActivity activity) {
        treeList = new ArrayList<TreeObject>();
        populateTreeSpinner(activity);
        activity.writeToFile(getHeading(activity), false, getHeading(activity));
    }

    public static int getNumObjects() {
        return (treeList.size());
    }

}


