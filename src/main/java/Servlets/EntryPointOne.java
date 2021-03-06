package Servlets;
import objects.PointNodeCollection;
import objects.TransportationVehicle;
import org.json.JSONArray;
import org.json.JSONObject;
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

        //setup of generic point independent variables
        String timeZone = null;
        TransportationVehicle vehicle = null;
        JSONObject startNode = null;
        JSONObject endNode = null;
        Long routesMaxTime = null;

        //setup vehicle default values
        Long vehicleCapacity = null;
        Integer numberOfVehicles = null;

        //request variables
        JSONObject jsonRequest = null;
        StringBuffer jb = new StringBuffer();

        //setup the pathParams info
        String info = request.getPathInfo();
        if(info != null){
            String[] pathParams = info.split("/");

            //checking for patameters *order doesn't matter at all for this*
            for(int i = 0; i < pathParams.length; i++){
                if (pathParams[i].equals("time")){
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

        //Getting out timezone and routesMaxTime
        if(pointTimeParameters){
            if(jsonRequest.has("timeZone"))
                timeZone = jsonRequest.getString("timeZone");
            if(jsonRequest.has("routesMaxTime"))
                routesMaxTime = jsonRequest.getLong("routesMaxTime");
        }
        //Getting out package parameters dependent info
        if(pointPackageParameters && jsonRequest.has("vehicleCapacity")){
            vehicleCapacity = jsonRequest.getLong("vehicleCapacity");
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

        //seeing if client added a number of vehicles
        if(jsonRequest.has("numberOfVehicles")){
            numberOfVehicles = jsonRequest.getInt("numberOfVehicles");
        }

        //setting up vehicle with variables
        vehicle = new TransportationVehicle(vehicleCapacity, numberOfVehicles);

        //getting Json Array of points
        JSONArray points = jsonRequest.getJSONArray("points");

        //First we create our PointNodeCollection which saves common data between nodes and an array of PointNodes
        PointNodeCollection pointNodeCollection = new PointNodeCollection(pointTimeParameters, pointHierarchyParameters, pointPackageParameters, timeZone, routeHasStartNode, routeHasEndNode, routesMaxTime);
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
        String startNode = null;
        String endNode = null;
        Long routesMaxTime = null;

        //setup vehicle default values
        Long vehicleCapacity = null;
        Integer numberOfVehicles = null;

        //setup the pathParams info
        String info = request.getPathInfo();
        if(info != null){
            String[] pathParams = info.split("/");

            //checking for patameters *order doesn't matter at all for this*
            for(int i = 0; i < pathParams.length; i++){
                if (pathParams[i].equals("time")){
                    pointTimeParameters = true;
                    //setup timezone param
                    timeZone = request.getParameter("timeZone");
                    //setup routesMaxtime
                    if(request.getParameter("routesMaxTime") != null)
                        routesMaxTime = Long.parseLong(request.getParameter("routesMaxTime"));
                }
                if (pathParams[i].equals("hierarchy")) {
                    pointHierarchyParameters = true;
                }
                if (pathParams[i].equals("packages")) {
                    pointPackageParameters = true;
                    if(request.getParameter("vehicleCapacity") != null)
                        vehicleCapacity = Long.parseLong(request.getParameter("vehicleCapacity"));

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

        //seeing if client added a number of vehicles
        if(request.getParameter("numberOfVehicles") != null)
            numberOfVehicles = Integer.parseInt(request.getParameter("numberOfVehicles"));

        //creating our vehicle
        vehicle = new TransportationVehicle(vehicleCapacity, numberOfVehicles);

        //First we create our PointNodeCollection which saves common data between nodes and an array of PointNodes
        PointNodeCollection pointNodeCollection = new PointNodeCollection(pointTimeParameters, pointHierarchyParameters, pointPackageParameters, timeZone, routeHasStartNode, routeHasEndNode, routesMaxTime);
        // we setup our PointNode arraylist inside our pointNodeCollection
        pointNodeCollection.setupPointNodes(points, startNode, endNode);

        //we get our shortest path as a JsonObject
        JSONObject returnResponse = ShortestPath.getShortestPath(pointNodeCollection, vehicle);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(returnResponse);
        out.flush();

    }
}
