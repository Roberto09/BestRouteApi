package Servlets;
import com.google.gson.JsonObject;
import com.google.maps.model.DistanceMatrix;
import objects.PointNodeCollection;
import objects.TransportationVehicle;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import util.GoogleMapsApi;
import util.ShortestPath;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class EntryPointOne extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        boolean pointTimeParameters = false;
        boolean pointHierarchyParameters = false;
        boolean pointPackageParameters = false;
        boolean routeHasStartNode = false;
        boolean routeHasEndNode = false;
        String timeZone = null;
        TransportationVehicle vehicle = null;
        JSONObject jsonRequest = null;

        //setup the pathParams info
        String info = request.getPathInfo();
        if(info != null){
            String[] pathParams = info.split("/");

            //checking for patameters *order doesn't matter at all for this*
            for(int i = 0; i < pathParams.length; i++){
                if (pathParams[i] == "time"){
                    pointTimeParameters = true;
                }
                if (pathParams[i].equals("hierarchy")) {
                    pointHierarchyParameters = true;
                }
                if (pathParams[i].equals("packages")) {
                    pointPackageParameters = true;
                }
            }
        }

        //receiving incoming Json
        StringBuffer jb = new StringBuffer();
        String line = null;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                jb.append(line);
        } catch (Exception e) { /*report an error*/ }

        try {
            //converting out stringbuffer to a json object
            jsonRequest = new JSONObject(jb.toString());
            System.out.print(jsonRequest);
        }
        catch (Exception e){
            System.out.println("Json not okay");
        }

        //Getting out timezone
        if(pointTimeParameters){
            timeZone = jsonRequest.getString("TimeZone");
        }

        //Getting out vehicle info
        if(pointPackageParameters){
            Long vehicleCapacity = jsonRequest.getLong("VehicleCapacity");
            if(vehicleCapacity != null)
                vehicle = new TransportationVehicle(vehicleCapacity);
        }

        //Seeing if the client added a start node and or an endnode
        JSONObject startNode = null;
        JSONObject endNode = null;

        if(jsonRequest.has("StartNode")) {
            startNode = jsonRequest.getJSONObject("SartNode");
            routeHasStartNode = true;
        }

        if(jsonRequest.has("EndNode")) {
            endNode = jsonRequest.getJSONObject("EndNode");
            routeHasEndNode = true;
        }

        //getting Json Array of points
        JSONArray points = jsonRequest.getJSONArray("points");

        //First we create our PointNodeCollection which saves common data between nodes and an array of PointNodes
        PointNodeCollection pointNodeCollection = new PointNodeCollection(pointTimeParameters, pointHierarchyParameters, pointPackageParameters, timeZone, routeHasStartNode, routeHasEndNode);
        // we setup our PointNode arraylist inside our pointNodeCollection
        pointNodeCollection.setUpPointNodesJson(points, startNode, endNode);

        //we get our shortest path as a JsonObject
        JSONObject returnResponse = ShortestPath.getShortestPath(pointNodeCollection, vehicle);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(returnResponse);
        out.flush();

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        //setup the boolean variables regarding the request
        boolean pointTimeParameters = false;
        boolean pointHierarchyParameters = false;
        boolean pointPackageParameters = false;
        boolean routeHasStartNode = false;
        boolean routeHasEndNode = false;

        //setup of generic point independent variables
        String timeZone = null;
        TransportationVehicle vehicle = null;

        //setup the pathParams info
        String info = request.getPathInfo();
        if(info != null){
            String[] pathParams = info.split("/");

            //checking for patameters *order doesn't matter at all for this*
            for(int i = 0; i < pathParams.length; i++){
                if (pathParams[i] == "time"){
                    pointTimeParameters = true;
                    //setup timezone param
                    timeZone = request.getParameter("timeZone");
                }
                if (pathParams[i].equals("hierarchy")) {
                    pointHierarchyParameters = true;
                }
                if (pathParams[i].equals("packages")) {
                    pointPackageParameters = true;
                    String vehicleCapacity = request.getParameter("vehicleCapacity");
                    if(vehicleCapacity!= null)
                        vehicle = new TransportationVehicle(Long.parseLong(vehicleCapacity));
                }
            }
        }

        //getting points
        String points[] = request.getParameterValues("point");
        String startNode = request.getParameter("startPoint");
        String endNode = request.getParameter("endPoint");

        //Seeing if the client added a start node and or an endnode
        if(startNode != null)
            routeHasStartNode = true;
        if(endNode != null)
            routeHasEndNode = true;


        //First we create our PointNodeCollection which saves common data between nodes and an array of PointNodes
        PointNodeCollection pointNodeCollection = new PointNodeCollection(pointTimeParameters, pointHierarchyParameters, pointPackageParameters, timeZone, routeHasStartNode, routeHasEndNode);
        // we setup our PointNode arraylist inside our pointNodeCollection
        pointNodeCollection.setupPointNodes(points, startNode, endNode);

        //we get our shortest path as a JsonObject
        JSONObject returnResponse = ShortestPath.getShortestPath(pointNodeCollection, vehicle);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(returnResponse);
        out.flush();

        String formatedDatetime = "01/01/2018 10:00:00 PST";
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss z");
        DateTime x = formatter.parseDateTime(formatedDatetime);

        String formatedDatetime2 = "01/01/2018 10:25:45 PST";
        DateTime x2 = formatter.parseDateTime(formatedDatetime2);

        int minutes = Minutes.minutesBetween(x, x2).getMinutes();
        System.out.println(minutes);
    }
}
