package objects;

import static util.ShortestPath.getShortestPath;

public class TransportationVehicle {
    //capacity variables for the vehicle
    private Long capacity = Long.MAX_VALUE; //default value is max long value capacity
    private Integer numberOfVehicles = 1; //default value is 1 Vehicle


    //TransportationVehicle class to set up the Vehicle / Vehicles used for the routing
    //this class in only useful for whenever the client asks for capacity constraints
    public TransportationVehicle(Long capacity, Integer numberOfVehicles){
        if(capacity != null)
            this.capacity = capacity;
        if(numberOfVehicles != null)
            this.numberOfVehicles = numberOfVehicles;
    }


    //getter and setter methods
    public Long getCapacity() {
        return this.capacity;
    }

    public void setCapacity(Long capacity) {
        this.capacity = capacity;
    }


    public Integer getNumberOfVehicles() {
        return numberOfVehicles;
    }

    public void setNumberOfVehicles(Integer numberOfVehicles) {
        this.numberOfVehicles = numberOfVehicles;
    }
}