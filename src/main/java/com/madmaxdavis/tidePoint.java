package com.madmaxdavis;

public class tidePoint
{
    String date;
    double tideLevel;

    public tidePoint(String d, double tide)
    {
        date = d;
        tideLevel = tide;
    }

    public  tidePoint()
    {
        date = "";
        tideLevel = 100;
    }

    public String getDate()
    {
        return date;
    }

    public double getTidePoint()
    {
        return tideLevel;
    }
}
