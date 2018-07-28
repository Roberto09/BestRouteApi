package util;

import objects.PointNodeCollection;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class TimeFunctions {

    public static long[][] setupTimeWindows(PointNodeCollection pointNodeCollection){

        long[][] timeWindows = new long[pointNodeCollection.pointNodes.length][];

        for(int i = 0; i < timeWindows.length; i++){
            //getting epoch time not in UTC but in the timezone given by the user divided by 1000 to get it in seconds
            timeWindows[i] = new long[]{DateTime.now().withZone(DateTimeZone.forID(pointNodeCollection.timeZone)).getMillis() / 1000, pointNodeCollection.pointNodes[i].getArrivalTime().getMillis() / 1000};
        }
        return timeWindows;

    }

}
