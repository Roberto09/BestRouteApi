package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.maps.model.DistanceMatrix;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.NodeEvaluator2;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import objects.PointNodeCollection;
import objects.TransportationVehicle;
import org.json.JSONArray;
import org.json.JSONObject;

public class ShortestPath {
    static { System.load("/home/roberto/Desktop/Tests/Test7/target/classes/libjniortools.so"); }

    //Distance callback which returns the distance between two given points
    public static class CreateTimeCallback extends NodeEvaluator2 {
        DistanceMatrix distanceMatrix;

        public CreateTimeCallback(DistanceMatrix distanceMatrix){
            super();
            this.distanceMatrix = distanceMatrix;
        }

        @Override
        public long run(int firstIndex, int secondIndex) {
            return (long)distanceMatrix.rows[firstIndex].elements[secondIndex].durationInTraffic.inSeconds;
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
            if(!pointNodeCollection.pointNodes[firstIndex].isPicksUpPackage())
                return (long) pointNodeCollection.pointNodes[firstIndex].getPackageWeight() * -1;
            return (long) pointNodeCollection.pointNodes[firstIndex].getPackageWeight();
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

    //get shortest path method which calculates the shortest path for the given points and conditions sent by the client
    public static JSONObject getShortestPath(PointNodeCollection pointNodeCollection, TransportationVehicle vehicle){

        //Creating Json Response and success attribute
        JSONObject result = new JSONObject();
        boolean success = false;

        DistanceMatrix distanceAndTimeMatrix = GoogleMapsApi.getDistanceAndTimeMatrix(pointNodeCollection);

        int tspSize = distanceAndTimeMatrix.destinationAddresses.length;
        int numRoutes = 1;

        //start point and return of the vehicles ins the routing
        int depot = 0;
        if(tspSize > 0) {
            //creating and seting up basic Routing Model
            RoutingModel routing;

            //setting up routing start nodes and end nodes according to client request
            if(pointNodeCollection.routeHasStartNode && pointNodeCollection.routeHasEndNode)
                routing = new RoutingModel(tspSize, numRoutes, new int[]{0},new int[]{1});
            else
                routing = new RoutingModel(tspSize, numRoutes, depot);

            RoutingSearchParameters searchParameters = RoutingModel.defaultSearchParameters();
            routing.setArcCostEvaluatorOfAllVehicles(new CreateTimeCallback(distanceAndTimeMatrix));

            //Adding CapacityConstraint to the routing model if neccesary
            if (pointNodeCollection.packagesParam)
                addCapacityContraint(routing, vehicle, new CreateDemandEvaluator(pointNodeCollection));

            //executing routing model and printing results
            Assignment assignment = routing.solveWithParameters(searchParameters);
            if(assignment != null){

                //setting success to true
                success = true;


                JSONArray routeAddresses = new JSONArray();
                JSONArray routeLatLng = new JSONArray();

                int routeNumber = 0;
                long index = routing.start(routeNumber);

                while(!routing.isEnd(index)){
                    routeAddresses.put(distanceAndTimeMatrix.destinationAddresses[routing.IndexToNode(index)]);
                    routeLatLng.put(pointNodeCollection.pointNodes[routing.IndexToNode(index)].getLatLngStr());
                    index = assignment.value(routing.nextVar(index));
                }
                routeAddresses.put(distanceAndTimeMatrix.destinationAddresses[routing.IndexToNode(index)]);
                routeLatLng.put(pointNodeCollection.pointNodes[routing.IndexToNode(index)].getLatLngStr());

                //Setting up json route object
                JSONObject route = new JSONObject();

                //setting total duration
                route.put("Total Duration:", assignment.objectiveValue());
                route.put("Route Addresses", routeAddresses);
                route.put("Route LatLng", routeLatLng);

                //adding route to the json result
                result.put("Route" + Integer.toString(routeNumber), route);
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
