package objects;

import com.google.maps.model.LatLng;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;

public class PointNode {

    public String timeZone = "GMT"; // default timezone unless the user changes it in the request
    private LatLng latLng; //latitude and longitude of the point
    private DateTime arrivalTime; //time at which it should arrive
    private Integer hierarchy; //hierarchy of the visit
    private boolean isStart = false; //true if the node is the start of the route
    private boolean isEnd = false; // true if the node is the end of the route
    private boolean picksUpPackage; //true if the transport has to pick a package in that poisition, false if it has to deliver one
    private int packageWeight; //weight of the package that needs to be picked up or delivered


    // receives string of type latitude,longitude|formatedDatetime|hierarchy|packageInfo
    // and also it receives 5 boolean values which allow us to understand the node purpose and the time zone the user used

    // format of rawString: nn.nnn,nnn.nnn|dd/MM/yyyy HH:mm:ss|n|n,tag
    // example: 24.872,-100.415|04/06/2018 04:27:32|3|42,PU
    public PointNode(String rawString, boolean timeParam, boolean hierarchyParam, boolean packagesParam, boolean start, boolean end, String tZone){

        //set up of node start and node end (if there is one)
        isStart = start;
        isEnd = end;

        int paramDivisor; //in other words the ammpersand '|' symbol which divides all params
        int commaPos; //position of comma variable is useful for some params

        //first lets get the latlng which is mandatory at any case
        if(timeParam || hierarchyParam || packagesParam)
            paramDivisor = rawString.indexOf('|');
        else
            paramDivisor = rawString.length();

        commaPos = rawString.indexOf(',');
        float lat = Float.parseFloat(rawString.substring(0, commaPos));
        float lng = Float.parseFloat(rawString.substring(commaPos + 1, paramDivisor));
        latLng = new LatLng(lat, lng);

        //if there's a time param we set it up
        if(timeParam){
            //then we get the formated Datetime
            rawString = rawString.substring(paramDivisor + 1, rawString.length());

            if(hierarchyParam || packagesParam)
                paramDivisor = rawString.indexOf('|');
            else
                paramDivisor = rawString.length();

            String formatedDatetime = rawString.substring(0, paramDivisor) + " " + timeZone; //we add the timezone
            DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss z");
            arrivalTime = formatter.parseDateTime(formatedDatetime);
        }

        //if there's a hierarchy param we set it up
        if(hierarchyParam){
            //then we set up the hierarchy
            rawString = rawString.substring(paramDivisor + 1, rawString.length());

            if(packagesParam)
                paramDivisor = rawString.indexOf('|');
            else
                paramDivisor = rawString.length();

            hierarchy = Integer.parseInt(rawString.substring(0, paramDivisor));
        }


        //if there's a packagesParam we set it up
        if(packagesParam){
            //finally we setup the package info
            String packageInfo = rawString.substring(paramDivisor + 1, rawString.length());
            commaPos = packageInfo.indexOf(',');
            packageWeight = Integer.parseInt(rawString.substring(0, commaPos));

            //see if we can find 'PU' here
            if(packageInfo.contains("PU"))
                picksUpPackage = true;
            else
                picksUpPackage = false;
        }

    }

    //regular creation of node
    public PointNode(LatLng latLng, DateTime arrivalTime, Integer hierarchy) {
        this.latLng = latLng;
        this.arrivalTime = arrivalTime;
        this.hierarchy = hierarchy;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public DateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(DateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Integer getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(Integer hierarchy) {
        this.hierarchy = hierarchy;
    }


    //method that sets up a array of point nodes according to the string nodes that we sent it
    public static PointNode[] setupPointNodes(String[] stringNodes, String startNode, String endNode, boolean timeParam, boolean hierarchyParam, boolean packagesParam, String tZone) {
        int size = stringNodes.length;

        if(startNode != null)
            size++;

        if(endNode != null)
            size++;

        PointNode[] pointNodes = new PointNode[size];

        int i = 0;
        for(; i < stringNodes.length; i++){
            pointNodes[i] = new PointNode(stringNodes[i], timeParam, hierarchyParam, packagesParam, false, false , tZone);
        }

        if(startNode != null)
            pointNodes[++i] = new PointNode(startNode, timeParam, hierarchyParam, packagesParam, true, false , tZone);

        if(endNode != null)
            pointNodes[++i] = new PointNode(startNode, timeParam, hierarchyParam, packagesParam, false, true , tZone);

        return pointNodes;
    }
}











































