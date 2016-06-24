package com.madmaxdavis;

import java.text.SimpleDateFormat;
import java.util.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.joda.time.*;


public class conditionData
{
    double currentTemp;
    double currentTideLevel;

    //public enum TideStatus
    //{
    //    UP, HIGH, DOWN, LOW
    //}

    SimpleDateFormat hours = new SimpleDateFormat("HH");
    SimpleDateFormat mins = new SimpleDateFormat("mm");

    DateTime predictedExtremaDateTime;
    double predictedExtremaTideLevel;

    public conditionData(double tide, double temp, DateTime dateTime, double tidePredict) // add tide status
    {
        currentTemp = temp;
        currentTideLevel = tide;
        predictedExtremaDateTime = dateTime;
        predictedExtremaTideLevel = tidePredict;
    }

    public double getTemp()
    {
        return currentTemp;
    }

    public double getCurTide()
    {
        return currentTideLevel;
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
