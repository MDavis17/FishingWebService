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

    DateTime predictedExtremaDateTime;
    double predictedExtremaTideLevel;

    public conditionData(double tide, double temp, DateTime dateTime, double tidePredict, String status) // add tide status
    {
        currentTemp = temp;
        currentTideLevel = tide;
        predictedExtremaDateTime = dateTime;
        predictedExtremaTideLevel = tidePredict;
        currentTideStatus = status;
    }

    public double getTemp()
    {
        return currentTemp;
    }

    public double getCurTide()
    {
        return currentTideLevel;
    }

    public String getTideStatus()
    {
        return currentTideStatus;
    }

    @JsonSerialize(using = CustomDateSerializer.class)
    public DateTime getDateTime()
    {
        return predictedExtremaDateTime;
    }

    public double getPredTide()
    {
        return predictedExtremaTideLevel;
    }

}
