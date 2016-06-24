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

    public class tidePoint
    {
        String date;
        double tideLevel;

        public tidePoint(String d, double tide)
        {
            date = d;
            tideLevel = tide;
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


    @RequestMapping("/current")
    public conditionData index() throws Exception
    {
        Date dnow = new Date();
        SimpleDateFormat date = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat time = new SimpleDateFormat("HH:mm");


        DateTime dt = new DateTime();
        double current_tide = getCurrentTidelevel();
        double current_temp = getCurrentTemp();

        conditionData data = new conditionData(current_tide,current_temp,dt,5.1);
        String tempUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?date=latest&station=9411340&product=air_temperature&units=english&time_zone=lst_ldt&application=ports_screen&format=json";
        String tideUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?date=latest&station=9411340&product=water_level&units=english&time_zone=lst_ldt&application=ports_screen&format=json&datum=STND";
        String tidePredUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?begin_date="+date.format(dnow)+"%20"+time.format(dnow)+"&range=24&station=9411340&product=predictions&units=english&time_zone=lst_ldt&format=json&datum=STND";

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


    //  TODO intent to consolidate these into getCurrentValue(String url)

    public double getCurrentTidelevel() throws Exception
    {
        String tideUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?date=latest&station=9411340&product=water_level&units=english&time_zone=lst_ldt&application=ports_screen&format=json&datum=STND";

        double current_tide = 0;

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getHTML(tideUrlString));

        JsonNode dataNode = root.path("data");
        for(JsonNode node: dataNode)
        {
            current_tide = node.path("v").asDouble();
        }

        return current_tide;
    }

    public double getCurrentTemp() throws Exception
    {
        String tempUrlString = "http://tidesandcurrents.noaa.gov/api/datagetter?date=latest&station=9411340&product=air_temperature&units=english&time_zone=lst_ldt&application=ports_screen&format=json";

        double current_temp = 0;

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getHTML(tempUrlString));

        JsonNode dataNode = root.path("data");
        for(JsonNode node: dataNode)
        {
            current_temp = node.path("v").asDouble();
        }

        return current_temp;
    }
}
