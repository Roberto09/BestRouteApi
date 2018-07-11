package objects;

import static util.ShortestPath.getShortestPath;

public class TransportationVehicle {
    //capacity variables for the vehicle
    private long capacity;
    private boolean hasCapacity;

    //TransportationVehicle class to set up the Vehicle / Vehicles used for the routing
    //this class in only useful for whenever the client asks for capacity constraints
    public TransportationVehicle(long capacity){
        this.capacity = capacity;
        if(this.capacity > 0)
            hasCapacity = true;
        else
            hasCapacity = false;
    }


    //getter and setter methods
    public long getCapacity() {
        return this.capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isHasCapacity() {

        return hasCapacity;
    }

    public void setHasCapacity(boolean hasCapacity) {
        this.hasCapacity = hasCapacity;
    }
}