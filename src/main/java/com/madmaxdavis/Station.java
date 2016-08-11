package com.madmaxdavis;

public class Station {
    String name;
    int ID;
    double lat;
    double lon;
    String state;

    public Station(String name, int id, double latitude, double longitude, String st) {
        this.name = name;
        ID = id;
        lat = latitude;
        lon = longitude;
        state = st;
    }

    public String getName() {return name;}
    public int getID() {return ID;}
    public double getLat() {return lat;}
    public double getLon() {return lon;}
    public String getState() {return state;}
}
