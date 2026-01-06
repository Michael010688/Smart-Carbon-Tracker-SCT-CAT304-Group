package org.example.utils;

public class CarbonRecord {
    // These are the data points to be displayed in your table
    private String date;
    private String activity; // Corresponds to the Activity column
    private String category; // Corresponds to Category (Icon)
    private double input;
    private double carbon;

    public CarbonRecord(String date, String activity, String category, double input, double carbon) {
        this.date = date;
        this.activity = activity;
        this.category = category;
        this.input = input;
        this.carbon = carbon;
    }

    // JavaFX TableView requires these Getter methods; otherwise, the cells will remain empty
    public String getDate() { return date; }
    public String getActivity() { return activity; }
    public String getCategory() { return category; }
    public double getInput() { return input; }
    public double getCarbon() { return carbon; }
}