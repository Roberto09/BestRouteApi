package util;

import objects.PointNodeCollection;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

public class TimeFunctions {

    public static long[][] setupTimeWindows(PointNodeCollection pointNodeCollection){
        long startTime = DateTime.now().withZone(DateTimeZone.forID(pointNodeCollection.timeZone)).toLocalDateTime().toDateTime(DateTimeZone.UTC).getMillis() / 1000;
        //long startTime = 1532817137L;

        System.out.println("Time Windows: TZ = " + startTime + " -----------------------------------------");
        long[][] timeWindows = new long[pointNodeCollection.pointNodes.length][];
        for(int i = 0; i < timeWindows.length; i++){
            //getting epoch time not in UTC but in the timezone given by the user divided by 1000 to get it in seconds
            timeWindows[i] = new long[]{startTime, pointNodeCollection.pointNodes[i].getArrivalTime().getMillis() / 1000};
            System.out.println(timeWindows[i][0] + ", " + timeWindows[i][1]);
        }
        return timeWindows;
    }
}
