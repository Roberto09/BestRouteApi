package util;

import com.google.maps.model.DistanceMatrix;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.NodeEvaluator2;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import objects.PointNodeCollection;

public class ShortestPath {
    static { System.load("/home/roberto/Desktop/Tests/Test7/target/classes/libjniortools.so"); }

    public static class CreateDistanceCallback extends NodeEvaluator2 {
        DistanceMatrix distanceMatrix;

        public CreateDistanceCallback(DistanceMatrix distanceMatrix){
            super();
            this.distanceMatrix = distanceMatrix;
        }

        @Override
        public long run(int firstIndex, int secondIndex) {
            return (int)distanceMatrix.rows[firstIndex].elements[secondIndex].durationInTraffic.inSeconds;
        }

    }

    public static void getShortestPath(PointNodeCollection pointNodeCollection){
        DistanceMatrix distanceMatrix = GoogleMapsApi.getDistances(pointNodeCollection);
        int tspSize = distanceMatrix.destinationAddresses.length;
        int numRoutes = 1;

        int depot = 0;
        if(tspSize > 0) {
            RoutingModel routing = new RoutingModel(tspSize, numRoutes, depot);
            RoutingSearchParameters searchParameters = RoutingModel.defaultSearchParameters();
            routing.setArcCostEvaluatorOfAllVehicles(new CreateDistanceCallback(distanceMatrix));
            Assignment assignment = routing.solveWithParameters(searchParameters);
            if(assignment != null){
                System.out.println("Total Duration: " + assignment.objectiveValue());
                int routeNumber = 0;
                String route = "";
                String route2 = "";
                long index = routing.start(routeNumber);
                while(!routing.isEnd(index)){
                    route += distanceMatrix.destinationAddresses[routing.IndexToNode(index)] + " -> ";
                    route2 += pointNodeCollection.pointNodes[routing.IndexToNode(index)].getLatLngStr() + " -> ";
                    index = assignment.value(routing.nextVar(index));
                }
                route += distanceMatrix.destinationAddresses[routing.IndexToNode(index)];
                route2 += pointNodeCollection.pointNodes[routing.IndexToNode(index)].getLatLngStr();
                System.out.println("Route: " + route);
                System.out.println("Route2: " + route2);
            }
            else
                System.out.println("No solution found");
        }
        else
            System.out.println("Specify an instance greater than 0");

    }
}
