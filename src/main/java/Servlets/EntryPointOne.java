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
import java.util.Date;

public class EntryPointOne extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        boolean pointTimeParameters = false;
        boolean pointHierarchyParameters = false;
        boolean pointPackageParameters = false;
        boolean routeHasStartNode = false;
        boolean routeHasEndNode = false;

        //setup of generic point independent variables
        String timeZone = null;
        TransportationVehicle vehicle = null;
        JSONObject startNode = null;
        JSONObject endNode = null;

        //request variables
        JSONObject jsonRequest = null;
        StringBuffer jb = new StringBuffer();

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
            timeZone = jsonRequest.getString("timeZone");
        }
        //Getting out vehicle info
        if(pointPackageParameters){
            Long vehicleCapacity = jsonRequest.getLong("vehicleCapacity");
            Integer numberOfVehicles = jsonRequest.getInt("numberOfVehicles");
            vehicle = new TransportationVehicle(vehicleCapacity, numberOfVehicles);
        }

        //Seeing if the client added a start node and or an endnode

        if(jsonRequest.has("startPoint")) {
            startNode = jsonRequest.getJSONObject("startPoint");
            routeHasStartNode = true;
        }

        if(jsonRequest.has("endPoint")) {
            endNode = jsonRequest.getJSONObject("endPoint");
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

        ShortestPath.showLibraryPaths();

        //setup the boolean variables regarding the request
        boolean pointTimeParameters = false;
        boolean pointHierarchyParameters = false;
        boolean pointPackageParameters = false;
        boolean routeHasStartNode = false;
        boolean routeHasEndNode = false;

        //setup of generic point independent variables
        String timeZone = null;
        TransportationVehicle vehicle = null;
        String startNode;
        String endNode;

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
                    String vehicleCapacityStr = request.getParameter("vehicleCapacity");
                    String numberOfVehiclesStr = request.getParameter("numberOfVehicles");
                    Long vehicleCapacity = null;
                    Integer numberOfVehicles = null;

                    if(vehicleCapacityStr != null)
                        vehicleCapacity = Long.parseLong(vehicleCapacityStr);

                    if(numberOfVehiclesStr != null)
                        numberOfVehicles = Integer.parseInt(numberOfVehiclesStr);

                    vehicle = new TransportationVehicle(vehicleCapacity, numberOfVehicles);

                }
            }
        }

        //getting points
        String points[] = request.getParameterValues("point");
        startNode = request.getParameter("startPoint");
        endNode = request.getParameter("endPoint");

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
        /*
        String formatedDatetime = "01/01/2018 10:00:00 PST";
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss z");
        DateTime x = formatter.parseDateTime(formatedDatetime);

        String formatedDatetime2 = "01/01/2018 10:25:45 PST";
        DateTime x2 = formatter.parseDateTime(formatedDatetime2);

        int minutes = Minutes.minutesBetween(x, x2).getMinutes();
        System.out.println(minutes);

        */
        String formatedDatetime = "01/01/2018 10:00:00 PST";
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss z");
        DateTime x = formatter.parseDateTime(formatedDatetime);
        System.out.println("Epoch time: " + x.getMillis() / 1000);
        System.out.println("Current time not epoch" + DateTime.now());

    }
}
