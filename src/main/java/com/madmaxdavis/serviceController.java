package com.madmaxdavis;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.joda.time.*;


@RestController
public class serviceController {

    /*
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
    }*/


    @RequestMapping("/current")
    public conditionData index() throws Exception
    {
        //may not need this date time stuff here if its only used in the getPredTide function...
        Date dnow = new Date();
        SimpleDateFormat date = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat time = new SimpleDateFormat("HH:mm");

        String tempUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?date=latest&station=9411340&product=air_temperature&units=english&time_zone=lst_ldt&application=ports_screen&format=json";
        String tideUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?date=latest&station=9411340&product=water_level&units=english&time_zone=lst_ldt&application=ports_screen&format=json&datum=STND";
        String tidePredUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?begin_date="+date.format(dnow)+"%20"+time.format(dnow)+"&range=24&station=9411340&product=predictions&units=english&time_zone=lst_ldt&format=json&datum=STND";



        DateTime dt = new DateTime();
        double current_tide = getCurrentValue(tideUrlString);
        double current_temp = getCurrentValue(tempUrlString);

        Vector<tidePoint> tidePredictions = getPredictedTides();

        int nextExtremeIndex = getNextExtremeIndex(tidePredictions);


        //double tide, double temp, DateTime dateTime, String status, tidePoint extreme
        //this now sets up the conditionData to hold the accurate current time, current temperature, the date/time, and the predicted tide level for the time of day
        conditionData data = new conditionData(current_tide,current_temp,dt,getTideStatus(0),tidePredictions.get(nextExtremeIndex));
        return data;
    }

    public static String getHTML(String urlString) throws Exception
    {
        StringBuilder result = new StringBuilder();

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null)
        {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }

    public double getCurrentValue(String url) throws Exception
    {
        double current_value = 0;

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getHTML(url));

        JsonNode dataNode = root.path("data");
        for(JsonNode node: dataNode)
        {
            current_value = node.path("v").asDouble();
        }

        return current_value;
    }

    public Vector<tidePoint> getPredictedTides() throws Exception
    {
        Date dnow = new Date();
        SimpleDateFormat date = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat time = new SimpleDateFormat("HH:mm");
        String tidePredUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?begin_date="+date.format(dnow)+"%20"+time.format(dnow)+"&range=24&station=9411340&product=predictions&units=english&time_zone=lst_ldt&format=json&datum=STND";

        Vector<tidePoint> tideNodes = new Vector();
        String tideDateTime;
        double level;



        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getHTML(tidePredUrlString));

        JsonNode dataNode = root.path("predictions");
        for(JsonNode node: dataNode)
        {
            tideDateTime = node.path("t").asText();
            level = node.path("v").asDouble();
            tidePoint newTidePoint = new tidePoint(tideDateTime,level);
            tideNodes.add(newTidePoint);
        }

        return tideNodes;
    }

    // TODO refactor to change the number status classifications to be innumerated types. also get rid of unnecessary conversions between types
    public String getTideStatus(int index) throws Exception // 0: out of bounds, 1: up, 2: down, 3: even, 4: high, 5: low, -1: nothing happened
    {
        int status = -1; // nothing happened in function
        tidePoint current = new tidePoint();
        Vector<tidePoint> tidePredictions = getPredictedTides();
        if(index >= tidePredictions.size() || index < 0)
            status = 0;   // out of bounds

        if(index+1 < tidePredictions.size()) // make sure we arent at the end of the vector
        {
            current = tidePredictions.get(index);
            tidePoint next = tidePredictions.get(index+1);
            // check all the rightward nodes to get an idea of the trend of the tide
            // TODO handle the case where this case inhibits an extreme from being discovered
            if(current.getTidePoint() == next.getTidePoint())
            {
                status = 3; // even
            }
            else if(current.getTidePoint() < next.getTidePoint())
            {
                status = 1; // up
            }
            else if(current.getTidePoint() > next.getTidePoint())
            {
                status = 2; // down
            }

            // check all the leftward nodes to see if they are peaks
            // TODO handle the case where a peak high or low is direclty at the first or last value in the vector of predictions
            /*
            if(index-1 < 0)
            {
                break; // at the first value, so just retain the status
            }*/

            if(index-1 >= 0)
            {

                tidePoint prev = tidePredictions.get(index - 1);
                if ((current.getTidePoint() < prev.getTidePoint() || current.getTidePoint() == prev.getTidePoint()) && status == 1) // minimum low
                {
                    status = 5;
                }
                else if ((current.getTidePoint() > prev.getTidePoint() || current.getTidePoint() == prev.getTidePoint()) && status == 2)  // peak high
                {
                    status = 4;
                }
            }
        }



        String tide_status;
        switch(status) // 0: out of bounds, 1: up, 2: down, 3: even, 4: high, 5: low, -1: nothing happened
        {
            case 1:
                tide_status = "up";
                break;
            case 2:
                tide_status = "down";
                break;
            case 3:
                tide_status = "even";
                break;
            case 4:
                tide_status = "high";
                break;
            case 5:
                tide_status = "low";
                break;
            default:
                tide_status = "unknown";
                break;
        }

        return tide_status;
    }

    public int getNextExtremeIndex(Vector<tidePoint> tidePredictions) throws Exception // returns the index of the next extreme in the vector
    {
        switch (getTideStatus(0))   // switch on the tide status of the current measurement
        {
            case "up":
            case "low":
                //look for high tide
                for(int i = 1; i < tidePredictions.size(); i++)
                {
                    if(getTideStatus(i) == "high")
                    {
                        return i;
                    }
                }
                break;

            case "high":
            case "down":
                //look for low tide
                for(int i = 1; i < tidePredictions.size(); i++)
                {
                    if(getTideStatus(i) == "low")
                    {
                        return i;
                    }
                }
                break;
            default:
                break;
        }


        return -1; // something went wrong
    }

    /*
    public boolean isAnExtreme(String extremeName, Vector<tidePoint> tidePredictions) throws Exception
    {
        for(int i = 1; i < tidePredictions.size(); i++)
        {
            if(getTideStatus(i) == extremeName)
            {
                return true;
            }
        }
        return false;
    }*/



}
