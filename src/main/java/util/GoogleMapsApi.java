package util;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import objects.PointNode;
import org.joda.time.DateTime;

import java.io.IOException;

public class GoogleMapsApi {
    public static String apiKey = "AIzaSyAjy7rrVcb2H3fwCSWR0W407OEW0-yfoLg";
    public static GeoApiContext context;

    public static void setUpGoogleApi(){
        context = new GeoApiContext.Builder().apiKey(apiKey).build(); }


    public static LatLng[] getLatLngFromPointNodes (PointNode[] pointNodes){
        LatLng[] latLngs = new LatLng[pointNodes.length];
        for(int i = 0; i < pointNodes.length; i++){
            latLngs[i] = pointNodes[i].getLatLng();
        }
        return latLngs;
    }


    public static DistanceMatrix getDistances(PointNode[] pointNodes){

        try{
            LatLng[] origins, destinations;
            origins = getLatLngFromPointNodes(pointNodes);
            destinations = origins;
            DistanceMatrixApiRequest request = DistanceMatrixApi.newRequest(context).mode(TravelMode.DRIVING);
            request.origins(origins);
            request.destinations(destinations);
            request.departureTime(DateTime.now());
            System.out.println(DateTime.now().toString());

            DistanceMatrix  distanceMatrix = request.await();

            System.out.println("------------");
            System.out.print(distanceMatrix.toString());
            System.out.println("------------");

            for(int i = 0; i < pointNodes.length; i++){
                for(int j = 0; j < pointNodes.length; j++){
                    System.out.println(distanceMatrix.rows[i].elements[j].durationInTraffic.inSeconds);
                }
            }

            return distanceMatrix;

        } catch (ApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;

    }
}
