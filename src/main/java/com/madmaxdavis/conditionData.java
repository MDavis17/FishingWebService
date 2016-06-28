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

    //public enum TideStatus
    //{
    //    UP, HIGH, DOWN, LOW
    //}

    SimpleDateFormat hours = new SimpleDateFormat("HH");
    SimpleDateFormat mins = new SimpleDateFormat("mm");

    DateTime currentDateTime;
    double nextExtremaTideLevel;
    String nextExtremaDateTime;
    //String currentDateTime;

    public conditionData(double tide, double temp, DateTime/*String*/ dateTime, String status, tidePoint extreme) // add tide status
    {
        currentTemp = temp;
        currentTideLevel = tide;
        currentDateTime = dateTime;
        nextExtremaTideLevel = extreme.getTidePoint();
        nextExtremaDateTime = extreme.getDate();
        currentTideStatus = status;


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
    public DateTime/*String*/ getCurrentDateTime()
    {
        return currentDateTime;
    }

    public double getNextExtremeTide()
    {
        return nextExtremaTideLevel;
    }

    public String getNextExtremeTime()
    {
        return nextExtremaDateTime;
    }

}
