package util;

import com.google.maps.model.DistanceMatrix;
import com.google.ortools.constraintsolver.*;
import objects.PointNodeCollection;
import objects.TransportationVehicle;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.StringTokenizer;

public class ShortestPath {

    //static {System.loadLibrary("jniortools");}

    public static void showLibraryPaths(){
        String property = System.getProperty("java.library.path");
        StringTokenizer parser = new StringTokenizer(property, ";");
        while (parser.hasMoreTokens()) {
            System.err.println(parser.nextToken());
        }
    }

    //Method that creates Json Object with node in route information
    private static JSONObject createNodeJson(String latLng, String name, Integer hierarchy, Long packageUnities, Long time, Long timeMin, Long timeMax){
        JSONObject object = new JSONObject();
        object.put("latLng", latLng);
        object.put("name", name);
        object.put("secondsElapsed", time);
        if(hierarchy != null)
            object.put("hierarchy", hierarchy);
        if(packageUnities != null)
            object.put("packageLoad", packageUnities);

        if(timeMin != null && timeMax != null){
            object.put("minimalArrivalTime", timeMin);
            object.put("maximumArrivalTime", timeMax);
        }
        return object;
    }

    //Distance callback which returns the time between two given points
    public static class CreateTimeCallback extends NodeEvaluator2 {
        DistanceMatrix distanceAndTimeMatrix;

        public CreateTimeCallback(DistanceMatrix distanceAndTimeMatrix){
            super();
            this.distanceAndTimeMatrix = distanceAndTimeMatrix;
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
            if(pointNodeCollection.pointNodes[firstIndex].getHierarchy().intValue() <= pointNodeCollection.pointNodes[secondIndex].getHierarchy().intValue() || secondIndex == pointNodeCollection.routeStartPosition)
                return 0;
            else
                return 1;
        }
    }

    //Evaluator for time window contraints
    public static class CreateTimeEvaluator extends NodeEvaluator2{
        DistanceMatrix distanceAndTimeMatrix;

        public CreateTimeEvaluator(DistanceMatrix distanceAndTimeMatrix){
            super();
            this.distanceAndTimeMatrix = distanceAndTimeMatrix;
        }

        @Override
        public long run(int firstIndex, int secondIndex) {
            return (long)distanceAndTimeMatrix.rows[firstIndex].elements[secondIndex].durationInTraffic.inSeconds;
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

    //Method that defines the dimensiton of the hierarchy constraint
    public static void addHierarchyConstraint(RoutingModel routing, CreateHierarchyEvaluator hierarchyEvaluator){
        routing.addDimension
                        (hierarchyEvaluator,
                        0,
                        0,
                        true, //start cumul at 0
                        "Hierarchy");
    }

    //Method that defines the dimenion of the TimeWindow contraint
    public static void addTimeWindowConstraint(RoutingModel routing, CreateTimeEvaluator timeEvaluator, TransportationVehicle vehicle, long[][] timeWindows, PointNodeCollection pointNodeCollection){
        //horizon in seconds
        Long horizon = (DateTime.now().withZone(DateTimeZone.forID(pointNodeCollection.timeZone)).toLocalDateTime().toDateTime(DateTimeZone.UTC).getMillis() / 1000) + pointNodeCollection.routesMaxTime;
        System.out.println("Timezone ->" + pointNodeCollection.timeZone);
        System.out.println("horizon time" + horizon);
        routing.addDimension
                        (timeEvaluator,
                        horizon,
                        horizon,
                        false, //don't force start cumul to zero since we are giving TW to start nodes
                        "TimeWindow");
        RoutingDimension timeWindowDimension = routing.getDimensionOrDie("TimeWindow");

        for(int i = 0; i < timeWindows.length; i++) {
            if(i == pointNodeCollection.routeStartPosition || i == pointNodeCollection.routeEndPosition)
                continue;
            long index = routing.NodeToIndex(i);
            System.out.println("twconstraint" + timeWindows[i][0] + " " + timeWindows[i][1]);
            timeWindowDimension.cumulVar(index).setRange(timeWindows[i][0], timeWindows[i][1]);
        }
        for(int i = 0; i < vehicle.getNumberOfVehicles(); i++){
            long index = routing.start(i);
            System.out.println("start index: " + index);
            timeWindowDimension.cumulVar(index).setRange(timeWindows[pointNodeCollection.routeStartPosition][0], timeWindows[pointNodeCollection.routeStartPosition][1]);
        }
    }

    //get shortest path method which calculates the shortest path for the given points and conditions sent by the client
    public static JSONObject getShortestPath(PointNodeCollection pointNodeCollection, TransportationVehicle vehicle){

        DistanceMatrix distanceAndTimeMatrix = GoogleMapsApi.getDistanceAndTimeMatrix(pointNodeCollection);

        int routingSize = distanceAndTimeMatrix.destinationAddresses.length;
        int numRoutes = vehicle.getNumberOfVehicles();

        //start point and return of the vehicles ins the routing
        int depot = 0;
        if(routingSize > 0) {
            //creating and seting up basic Routing Model
            RoutingModel routing;

            //creating arrays of startNodesPositions and endNodesPositions
            int[] startNodesPositionArray = new int[vehicle.getNumberOfVehicles()], endNodesPositionArray = new int[vehicle.getNumberOfVehicles()];
            Arrays.fill(startNodesPositionArray, pointNodeCollection.routeStartPosition);
            Arrays.fill(endNodesPositionArray, pointNodeCollection.routeEndPosition);
            System.out.print("n vehicles: " + vehicle.getNumberOfVehicles() + " ----- startPoint: " + pointNodeCollection.routeStartPosition + " --------- endpoint" + pointNodeCollection.routeEndPosition);

            //setting up routing start nodes and end nodes according to client request
            routing = new RoutingModel(routingSize, numRoutes, startNodesPositionArray, endNodesPositionArray);

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

            //Adding TimeWindowConstraint to the routing model if neccesary
            if (pointNodeCollection.timeParam)
                addTimeWindowConstraint(routing, new CreateTimeEvaluator(distanceAndTimeMatrix), vehicle, TimeFunctions.setupTimeWindows(pointNodeCollection), pointNodeCollection);

            //executing routing model and printing results
            Assignment assignment = routing.solveWithParameters(searchParameters);
            if(assignment != null){
                //getting results and returning them
                return getJsonResults(routing, assignment, numRoutes, distanceAndTimeMatrix, pointNodeCollection);
            }
            else {
                System.out.println("No solution found");
            }
        }
        else
            System.out.println("Specify an instance greater than 0");

        //returning result if something failed and adding success status
        JSONObject result = new JSONObject();
        result.put("success", false);
        return result;
    }

    //get Json Results method which allows us to translate our results to a final json that will be returned to the user in the getShortestPath method
    private static JSONObject getJsonResults(RoutingModel routing, Assignment assignment, int numRoutes, DistanceMatrix distanceAndTimeMatrix, PointNodeCollection pointNodeCollection){
        //creating our json object
        JSONObject result = new JSONObject();

        //setting success to true
        long totalDuration = 0;

        //getting solution dimentions for further inspection
        RoutingDimension capacityDimension = null;
        RoutingDimension timeWindowDimension = null;
        if(pointNodeCollection.packagesParam)
            capacityDimension = routing.getDimensionOrDie("Capacity");

        if(pointNodeCollection.timeParam)
            timeWindowDimension = routing.getDimensionOrDie("TimeWindow");

        //for loop to get the best path for each route
        for(int routeNum = 0; routeNum < numRoutes; routeNum ++) {
            JSONArray routeResults = new JSONArray();
            long routeDuration  = 0;
            long index = routing.start(routeNum);

            //individual route variables
            Integer nodeIndex;
            Integer nextNodeIndex;
            String latLng;
            String name;
            Integer hierarchy = null;
            Long packageUnities = null;
            Long timeMin = null, timeMax = null;

            while (!routing.isEnd(index)) {
                //getting index of the routing node
                nodeIndex = routing.IndexToNode(index);
                //getting index of the next routing node
                nextNodeIndex = routing.IndexToNode(assignment.value(routing.nextVar(index)));

                //setting our name
                name = distanceAndTimeMatrix.destinationAddresses[nodeIndex];

                //setting our latLng
                latLng = pointNodeCollection.pointNodes[nodeIndex].getLatLngStr();

                //setting our hierarchy
                if(pointNodeCollection.hierarchyParam)
                    hierarchy = pointNodeCollection.pointNodes[nodeIndex].getHierarchy();

                //setting out package unities cumul
                if(pointNodeCollection.packagesParam){
                    IntVar loadVar = capacityDimension.cumulVar(index);
                    packageUnities = assignment.value(loadVar);
                }

                //setting out time windows
                if(pointNodeCollection.timeParam){
                    IntVar timeWindowVar = timeWindowDimension.cumulVar(index);
                    timeMin = assignment.min(timeWindowVar);
                    timeMax = assignment.max(timeWindowVar);
                }

                //adding resultNode to our routeResults
                routeResults.put(createNodeJson(latLng, name, hierarchy, packageUnities, routeDuration, timeMin, timeMax));
                //adding our route duration the duration in seconds of the nodeIndex to the nextNodeIndex;
                routeDuration += distanceAndTimeMatrix.rows[nodeIndex].elements[nextNodeIndex].durationInTraffic.inSeconds;

                //updating our index
                index = assignment.value(routing.nextVar(index));
            }
            //adding to our totalDuration
            totalDuration += routeDuration;

            //setting our nodeIndex variable to our last node
            nodeIndex = routing.IndexToNode(index);

            //setting out name
            name = distanceAndTimeMatrix.destinationAddresses[nodeIndex];

            //setting our latLng
            latLng = pointNodeCollection.pointNodes[nodeIndex].getLatLngStr();

            //setting our hierarchy
            if(pointNodeCollection.hierarchyParam)
                hierarchy = pointNodeCollection.pointNodes[nodeIndex].getHierarchy();

            //setting out package unities
            if(pointNodeCollection.packagesParam){
                IntVar loadVar = capacityDimension.cumulVar(index);
                packageUnities = assignment.value(loadVar);
            }

            //setting out time windows
            if(pointNodeCollection.timeParam){
                IntVar timeWindowVar = timeWindowDimension.cumulVar(index);
                timeMin = assignment.min(timeWindowVar);
                timeMax = assignment.max(timeWindowVar);
            }

            //adding resultNode to our routeResults
            routeResults.put(createNodeJson(latLng, name, hierarchy, packageUnities, routeDuration, timeMin, timeMax));

            //Setting up json route object
            JSONObject route = new JSONObject();

            //setting total duration
            route.put("duration:", routeDuration);
            route.put("route", routeResults);

            //adding route to the json result
            result.put("route" + Integer.toString(routeNum), route);
        }
        result.put("total Duration", totalDuration);
        result.put("success", true);
        System.out.println("Total duration: " + totalDuration + " ---- Objective Value: " + assignment.objectiveValue());
        return result;
    }
}