package com.madmaxdavis;

import java.text.SimpleDateFormat;
import java.util.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.joda.time.*;


public class conditionData
{
    double currentTemp;
    double currentTideLevel;
    String currentTideStatus;

    public enum TideStatus
    {
        UP, HIGH, DOWN, LOW
    }

    SimpleDateFormat hours = new SimpleDateFormat("HH");
    SimpleDateFormat mins = new SimpleDateFormat("mm");

    DateTime currentDateTime;
    double nextExtremaTideLevel;
    String nextExtremaDateTime;
    double nextExtremeTemp;
    String stationName;
    double stationLat;
    double stationLon;
    String cityName;

    public conditionData(double tide, double temp, DateTime dateTime, String status, tidePoint extreme, double nextTemp,String sName, double lat, double lon, String cName) // add tide status
    {
        currentTemp = temp;
        currentTideLevel = tide;
        currentDateTime = dateTime;
        nextExtremaTideLevel = extreme.getTidePoint();
        nextExtremaDateTime = extreme.getDate();
        currentTideStatus = status;
        nextExtremeTemp = nextTemp;
        stationName = sName;
        stationLat = lat;
        stationLon = lon;
        cityName = cName;

    }

    public double getCurrentTemp()
    {
        return currentTemp;
    }

    public double getCurrentTide()
    {
        return currentTideLevel;
    }

    public String getCurrentTideStatus()
    {
        return currentTideStatus;
    }

    @JsonSerialize(using = CustomDateSerializer.class)
    public DateTime getCurrentDateTime()
    {
        return new DateTime(DateTimeZone.forID("America/Los_Angeles"));
    }

    public double getNextExtremeTide()
    {
        return nextExtremaTideLevel;
    }

    public String getNextExtremeTime()
    {
        return nextExtremaDateTime;
    }

    public double getNextTemp() { return nextExtremeTemp; }

    public String getStationName() { return stationName; }

    public String getCityName() { return cityName; }

    public double getStationLat() {return stationLat;}

    public double getStationLon() {return stationLon;}

}
