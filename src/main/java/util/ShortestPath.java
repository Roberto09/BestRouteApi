package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.maps.model.DistanceMatrix;
import com.google.ortools.constraintsolver.*;
import objects.PointNodeCollection;
import objects.TransportationVehicle;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

public class ShortestPath {
    //static { System.load("/home/roberto/Desktop/Tests/Test7/target/classes/libjniortools.so"); }

    static {System.loadLibrary("jniortools");}

    public static void showLibraryPaths(){
        String property = System.getProperty("java.library.path");
        StringTokenizer parser = new StringTokenizer(property, ";");
        while (parser.hasMoreTokens()) {
            System.err.println(parser.nextToken());
        }
    }


    //Distance callback which returns the distance between two given points
    public static class CreateTimeCallback extends NodeEvaluator2 {
        DistanceMatrix distanceAndTimeMatrix;

        public CreateTimeCallback(DistanceMatrix distanceMatrix){
            super();
            this.distanceAndTimeMatrix = distanceMatrix;
        }

        @Override
        public long run(int firstIndex, int secondIndex) {
            return (long)distanceAndTimeMatrix.rows[firstIndex].elements[secondIndex].durationInTraffic.inSeconds;
        }

    }

   //Demand Evaluator for capacity constraints
   public static class CreateDemandEvaluator extends NodeEvaluator2{
        PointNodeCollection pointNodeCollection;

        public CreateDemandEvaluator(PointNodeCollection pointNodeCollection){
            this.pointNodeCollection = pointNodeCollection;
        }


       @Override
       public long run(int firstIndex, int secondIndex) {
            long value;
            if(!pointNodeCollection.pointNodes[firstIndex].isPicksUpPackage())
                value = (long) pointNodeCollection.pointNodes[firstIndex].getPackageWeight() * -1;
            else
                value =  (long) pointNodeCollection.pointNodes[firstIndex].getPackageWeight();
            System.out.println(value);
            return value;
       }
   }


    //Evaluator for hierarchy constraints
    public static class CreateHierarchyEvaluator extends NodeEvaluator2{
        PointNodeCollection pointNodeCollection;

        public CreateHierarchyEvaluator(PointNodeCollection pointNodeCollection){
            this.pointNodeCollection = pointNodeCollection;
        }

        @Override
        public long run(int firstIndex, int secondIndex) {
            if(pointNodeCollection.pointNodes[firstIndex].getHierarchy() <= pointNodeCollection.pointNodes[secondIndex].getHierarchy() || secondIndex == pointNodeCollection.routeStartPosition)
                return 0;
            else
                return 1;
        }
    }

    //Method that defines the dimension of the capacity constraint
    public static void addCapacityContraint(RoutingModel routing, TransportationVehicle vehicle, CreateDemandEvaluator demandEvaluator){
        routing.addDimension
                        (demandEvaluator,
                        0,
                        vehicle.getCapacity(),
                        true, //start cumul at 0
                        "Capacity");
    }

    //Method that defines the dimensiton of thehierarchy constraint
    public static void addHierarchyConstraint(RoutingModel routing, CreateHierarchyEvaluator hierarchyEvaluator){
        routing.addDimension
                        (hierarchyEvaluator,
                        0,
                        0,
                        true, //start cumul at 0
                        "Hierarchy");
    }

    //get shortest path method which calculates the shortest path for the given points and conditions sent by the client
    public static JSONObject getShortestPath(PointNodeCollection pointNodeCollection, TransportationVehicle vehicle){

        //Creating Json Response and success attribute
        JSONObject result = new JSONObject();
        boolean success = false;

        DistanceMatrix distanceAndTimeMatrix = GoogleMapsApi.getDistanceAndTimeMatrix(pointNodeCollection);

        int tspSize = distanceAndTimeMatrix.destinationAddresses.length;
        int numRoutes = vehicle.getNumberOfVehicles();

        //start point and return of the vehicles ins the routing
        int depot = 0;
        if(tspSize > 0) {
            //creating and seting up basic Routing Model
            RoutingModel routing;

            //creating arrays of startNodesPositions and endNodesPositions
            int[] startNodesPositionArray = new int[vehicle.getNumberOfVehicles()], endNodesPositionArray = new int[vehicle.getNumberOfVehicles()];
            Arrays.fill(startNodesPositionArray, pointNodeCollection.routeStartPosition);
            Arrays.fill(endNodesPositionArray, pointNodeCollection.routeEndPosition);
            System.out.print("n vehicles: " + vehicle.getNumberOfVehicles() + " ----- startPoint: " + pointNodeCollection.routeStartPosition + " --------- endpoint" + pointNodeCollection.routeEndPosition);

            //setting up routing start nodes and end nodes according to client request
            routing = new RoutingModel(tspSize, numRoutes, startNodesPositionArray, endNodesPositionArray);

            //Creating search parameters for the routing model and seting the callback for the distance/time between nodes
            RoutingSearchParameters searchParameters = RoutingSearchParameters.newBuilder()
                    .mergeFrom(RoutingModel.defaultSearchParameters())
                    .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                    .build();
            routing.setArcCostEvaluatorOfAllVehicles(new CreateTimeCallback(distanceAndTimeMatrix));

            //Adding CapacityConstraint to the routing model if neccesary
            if (pointNodeCollection.packagesParam)
                addCapacityContraint(routing, vehicle, new CreateDemandEvaluator(pointNodeCollection));

            //Adding HierrarchyConstraint to the routign model if neccesary
            if(pointNodeCollection.hierarchyParam)
                addHierarchyConstraint(routing, new CreateHierarchyEvaluator(pointNodeCollection));

            //executing routing model and printing results
            Assignment assignment = routing.solveWithParameters(searchParameters);
            if(assignment != null){

                //setting success to true
                success = true;
                long totalDuration = 0;


                //getting solution dimentions for further inspection
                RoutingDimension capacityDimension = routing.getDimensionOrDie("Capacity");

                //for loop to get the best path for each route
                for(int routeNum = 0; routeNum < numRoutes; routeNum ++) {
                    JSONArray routeAddresses = new JSONArray();
                    JSONArray routeLatLng = new JSONArray();

                    StringBuffer vehicleLoad = new StringBuffer();

                    long routeDuration  = 0;

                    long index = routing.start(routeNum);

                    while (!routing.isEnd(index)) {
                        //getting index of the routing node
                        int nodeIndex = routing.IndexToNode(index);
                        //getting index of the next routing node
                        int nextNodeIndex = routing.IndexToNode(assignment.value(routing.nextVar(index)));

                        //adding our route duration the duration in seconds of the nodeIndex to the nextNodeIndex;
                        routeDuration += distanceAndTimeMatrix.rows[nodeIndex].elements[nextNodeIndex].durationInTraffic.inSeconds;

                        //adding the address to our route addresses array
                        routeAddresses.put(distanceAndTimeMatrix.destinationAddresses[nodeIndex]);
                        //adding the latLng to our route latLng array
                        routeLatLng.put(pointNodeCollection.pointNodes[nodeIndex].getLatLngStr());

                        //adding to our route load
                        IntVar loadVar2 = capacityDimension.cumulVar(index);
                        Long routeLoad = assignment.value(loadVar2);
                        vehicleLoad.append(routeLoad).append("->");

                        //updating our index
                        index = assignment.value(routing.nextVar(index));
                    }
                    //adding to our totalDuration
                    totalDuration += routeDuration;

                    //adding the last node adress and latLng to their arrays (such as done in the while loop)
                    routeAddresses.put(distanceAndTimeMatrix.destinationAddresses[routing.IndexToNode(index)]);
                    routeLatLng.put(pointNodeCollection.pointNodes[routing.IndexToNode(index)].getLatLngStr());

                    //adding to our route load
                    IntVar loadVar2 = capacityDimension.cumulVar(index);
                    Long routeLoad = assignment.value(loadVar2);
                    vehicleLoad.append(routeLoad);

                    //Setting up json route object
                    JSONObject route = new JSONObject();

                    //setting total duration
                    route.put("duration:", routeDuration);
                    route.put("routeAddresses", routeAddresses);
                    route.put("routeLatLng", routeLatLng);

                    //adding route to the json result
                    result.put("route" + Integer.toString(routeNum), route);

                    System.out.println(vehicleLoad);
                }
                result.put("Total Duration", totalDuration);
                System.out.println("Total duration: " + totalDuration + " ---- Objective Value: " + assignment.objectiveValue());
            }
            else {
                System.out.println("No solution found");
            }
        }
        else
            System.out.println("Specify an instance greater than 0");

        //returning result and adding success status
        result.put("success", success);
        return result;
    }
}