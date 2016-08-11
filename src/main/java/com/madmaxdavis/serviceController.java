package com.madmaxdavis;


import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.*;
import java.io.*;
import java.util.*;
import org.joda.time.*;
import org.joda.time.DateTimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.lang.Math;

import org.w3c.dom.*;


@RestController
public class serviceController {

    @RequestMapping("/current/{latitude},{longitude:.+}")
    public @ResponseBody conditionData index(@PathVariable("latitude") double latitude, @PathVariable("longitude") double longitude) throws Exception
    {
        double searchLat = latitude;
        double searchLong = longitude;

        // get nearest station to the provided lat-long
        Station nearestStation = getNearestStation(searchLat,searchLong);

        // list of stations and their ids: https://tidesandcurrents.noaa.gov/stations.html?type=All%20Stations&sort=0#California
        //9414290 san francisco
        //9411340 santa barbara
        //9410840 santa monica

        DateTime dnow = new DateTime(DateTimeZone.forID("America/Los_Angeles"));
        DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy%20HH:mm");

        String tempUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?date=latest&station="+nearestStation.getID()+"&product=air_temperature&units=english&time_zone=lst_ldt&application=ports_screen&format=json";
        String tideUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?date=latest&station="+nearestStation.getID()+"&product=water_level&units=english&time_zone=lst_ldt&application=ports_screen&format=json&datum=MLLW";
        String tidePredUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?begin_date="+dnow.toString(dtf)+"&range=24&station="+nearestStation.getID()+"&product=predictions&units=english&time_zone=lst_ldt&format=json&datum=MLLW";

        double current_tide = getCurrentValue(tideUrlString);
        double current_temp = getCurrentValue(tempUrlString);
        String stationLat = getLat(tideUrlString);
        String stationLong = getLon(tideUrlString);

        // getting the city that the station is in
        String locationUrl = "https://maps.googleapis.com/maps/api/geocode/json?latlng="+stationLat+","+stationLong+"&key=AIzaSyB68dw86kU2w99PEiOMsmuRBpyj0Ek-128";

        String stationName = nearestStation.getName().replace(" ","_");
        String cityName = getLocationData(locationUrl,"city").replace(" ","_");
        String stateAbrv = getLocationData(locationUrl,"state");

        String nextTempUrl = "http://api.wunderground.com/api/52d8fa4f8cf52c6a/hourly/q/"+stateAbrv+"/"+cityName+".json";

        Vector<tidePoint> tidePredictions = getPredictedTides();

        int nextExtremeIndex = getNextExtremeIndex(tidePredictions);
        tidePoint nextExtreme = tidePredictions.get(nextExtremeIndex);


        String nextTime = nextExtreme.getDate();

        String year = nextTime.substring(0,4);
        String month = nextTime.substring(5,7);
        String day = nextTime.substring(8,10);
        String time= nextTime.substring(11,16);
        String hour = time.substring(0,2);

        // get temp at time attached to the next extreme tide point
        int next_temp = getNextTemp(nextTempUrl,Integer.parseInt(hour));


        //this now sets up the conditionData to hold the accurate current time, current temperature, the date/time, and the predicted tide level for the time of day
        conditionData data = new conditionData(current_tide,current_temp,dnow,getTideStatus(0),nextExtreme,next_temp,stationName,cityName);
        return data;
    }

    // TODO: put this function and the distance into a separate controller
    public static Station getNearestStation(double lat, double lon) throws Exception {

        URL url = new URL("http://opendap.co-ops.nos.noaa.gov/stations/stationsXML.jsp");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(conn.getInputStream());
        NodeList stationsList = doc.getElementsByTagName("station");

        int nearestStationID = 0;
        double nearestStationDistance = 0;
        String nearestStationName = "";
        double nearestLat = 0;
        double nearestlong = 0;
        String nearestStationState = "";

        for(int i = 0; i < stationsList.getLength(); i++) {
            Node node = stationsList.item(i);

            String stName = node.getAttributes().getNamedItem("name").getNodeValue();
            int stID = Integer.parseInt(node.getAttributes().getNamedItem("ID").getNodeValue());


            Element e = (Element) node;
            Element metadata = (Element) e.getElementsByTagName("metadata").item(0);
            Element loc = (Element) metadata.getElementsByTagName("location").item(0);

            double latitude = Double.parseDouble(loc.getElementsByTagName("lat").item(0).getChildNodes().item(0).getNodeValue());
            double longitude = Double.parseDouble(loc.getElementsByTagName("long").item(0).getChildNodes().item(0).getNodeValue());
            String state = "";
            if(loc.getElementsByTagName("state").item(0).getChildNodes().item(0) != null)
                state = loc.getElementsByTagName("state").item(0).getChildNodes().item(0).getNodeValue();
            double currentDistance = distance(lat,lon,latitude,longitude);

            if(nearestStationID == 0) {
                nearestStationID = stID;
                nearestStationDistance = currentDistance;
            }
            else if(currentDistance < nearestStationDistance) {
                nearestStationID = stID;
                nearestStationDistance = currentDistance;
                nearestStationName = stName;
                nearestLat = latitude;
                nearestlong = longitude;
                nearestStationState = state;
            }
        }
        return new Station(nearestStationName,nearestStationID,nearestLat,nearestlong,nearestStationState);
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        return Math.sqrt(Math.pow((lat1-lat2),2) + Math.pow((lon1-lon2),2));
    }

    public static String getViaHTTP(String urlString) throws Exception
    {
        StringBuilder result = new StringBuilder();

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        int a = 0;
        String line;
        while ((line = rd.readLine()) != null)
        {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }

    public int getNextTemp(String url, int exHour) throws Exception
    {
        int temp = 0;
        int itrHour;

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getViaHTTP(url));

        JsonNode dataNode = root.path("hourly_forecast");
        for(JsonNode node: dataNode)
        {
            JsonNode currentNode = node.path("FCTTIME");
            itrHour = currentNode.path("hour").asInt();
            if(itrHour == exHour)
            {
                currentNode = node.path("temp");
                temp = currentNode.path("english").asInt();
                return temp;
            }
        }

        return temp;
    }

    public double getCurrentValue(String url) throws Exception
    {
        double current_value = 0;

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getViaHTTP(url));

        JsonNode dataNode = root.path("data");
        for(JsonNode node: dataNode)
        {
            current_value = node.path("v").asDouble();
        }

        return current_value;
    }

    public String getLocationData(String url, String field) throws Exception
    {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getViaHTTP(url));

        JsonNode dataNode = root.path("results");
        int a = 0;
        for(JsonNode outter_node: dataNode)
        {
            JsonNode addressNode = outter_node.path("address_components");
            for (JsonNode inner_node : addressNode)
            {
                JsonNode typeNode = inner_node.path("types");
                for (JsonNode typeValue : typeNode)
                {
                    if (field == "city" && typeValue.asText().contains("locality"))
                    {
                        return inner_node.path("long_name").asText();
                    }
                    else if (field == "state" && typeValue.asText().contains("administrative_area_level_1"))
                    {
                        return inner_node.path("short_name").asText();
                    }
                }
            }
        }

        return "";
    }

    public String getLat(String url) throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getViaHTTP(url));

        JsonNode dataNode = root.path("metadata");
        return dataNode.path("lat").asText();
    }

    public String getLon(String url) throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getViaHTTP(url));

        JsonNode dataNode = root.path("metadata");
        return dataNode.path("lon").asText();
    }

    public Vector<tidePoint> getPredictedTides() throws Exception
    {
        DateTime dnow = new DateTime(DateTimeZone.forID("America/Los_Angeles"));
        DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy%20HH:mm");
        String tidePredUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?begin_date="+dnow.toString(dtf)+"&range=24&station=9411340&product=predictions&units=english&time_zone=lst_ldt&format=json&datum=MLLW";

        Vector<tidePoint> tideNodes = new Vector();
        String tideDateTime;
        double level;

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getViaHTTP(tidePredUrlString));

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

    // TODO refactor to change the number status classifications to be enumerated types. also get rid of unnecessary conversions between types
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
            // TODO handle the case where a peak high or low is directly at the first or last value in the vector of predictions

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
}
