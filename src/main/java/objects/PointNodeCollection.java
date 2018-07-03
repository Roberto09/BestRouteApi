package objects;

import com.google.maps.model.LatLng;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


//PointNodeCollection class that encapsulates PointNode class so that all PointNode instances share the same "global" variables (timeParam, hierarchyParam, packagesParam and timeZone)
//whithout the need to create static variables since this global variables should only be common within the same thread and not between threads as static variables do
public class PointNodeCollection {
    //public variabes pertaining to all the point nodes
    public boolean timeParam, hierarchyParam, packagesParam;
    public String timeZone;
    public PointNode[] pointNodes;


    public PointNodeCollection(boolean timeParam, boolean hierarchyParam, boolean packagesParam, String timeZone) {
        this.timeParam = timeParam;
        this.hierarchyParam = hierarchyParam;
        this.packagesParam = packagesParam;
        this.timeZone = timeZone;
    }

    public class PointNode{

        private String latLngStr; //latitude and longitude in string format
        private LatLng latLng; //latitude and longitude of the point
        private DateTime arrivalTime; //time at which it should arrive
        private Integer hierarchy; //hierarchy of the visit
        private boolean isStart = false; //true if the node is the start of the route
        private boolean isEnd = false; // true if the node is the end of the route
        private boolean picksUpPackage; //true if the transport has to pick a package in that poisition, false if it has to deliver one
        private int packageWeight; //weight of the package that needs to be picked up or delivered


        // Constructor receives string of type latitude,longitude|formatedDatetime|hierarchy|packageInfo and also 2 boolean variables which determine
        // whether the point node is the start, the end or it's between the route

        // format of rawString: nn.nnn,nnn.nnn|dd/MM/yyyy HH:mm:ss|n|n,tag
        // example: 24.872,-100.415|04/06/2018 04:27:32|3|42,PU
        public PointNode(String rawString, boolean start, boolean end){

            //set up of node start and node end (if there is one)
            isStart = start;
            isEnd = end;

            int paramDivisor; //in other words the ammpersand '|' symbol which divides all params
            int commaPos; //position of comma variable is useful for some params

            //first lets get the latlng which is mandatory at any case
            if(timeParam || hierarchyParam || packagesParam)
                paramDivisor = rawString.indexOf('|');
            else
                paramDivisor = rawString.length();

            commaPos = rawString.indexOf(',');
            float lat = Float.parseFloat(rawString.substring(0, commaPos));
            float lng = Float.parseFloat(rawString.substring(commaPos + 1, paramDivisor));
            latLngStr = rawString.substring(0, paramDivisor);
            latLng = new LatLng(lat, lng);

            //if there's a time param we set it up
            if(timeParam){
                //then we get the formated Datetime
                rawString = rawString.substring(paramDivisor + 1, rawString.length());

                if(hierarchyParam || packagesParam)
                    paramDivisor = rawString.indexOf('|');
                else
                    paramDivisor = rawString.length();

                String formatedDatetime = rawString.substring(0, paramDivisor) + " " + timeZone; //we add the timezone
                DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss z");
                arrivalTime = formatter.parseDateTime(formatedDatetime);
            }

            //if there's a hierarchy param we set it up
            if(hierarchyParam){
                //then we set up the hierarchy
                rawString = rawString.substring(paramDivisor + 1, rawString.length());

                if(packagesParam)
                    paramDivisor = rawString.indexOf('|');
                else
                    paramDivisor = rawString.length();

                hierarchy = Integer.parseInt(rawString.substring(0, paramDivisor));
            }


            //if there's a packagesParam we set it up
            if(packagesParam){
                //finally we setup the package info
                String packageInfo = rawString.substring(paramDivisor + 1, rawString.length());
                commaPos = packageInfo.indexOf(',');
                packageWeight = Integer.parseInt(rawString.substring(0, commaPos));

                //see if we can find 'PU' here
                if(packageInfo.contains("PU"))
                    picksUpPackage = true;
                else
                    picksUpPackage = false;
            }

        }

        public String getLatLngStr() {
            return latLngStr;
        }

        public void setLatLngStr(String latLngStr) {
            this.latLngStr = latLngStr;
        }

        public LatLng getLatLng() {
            return latLng;
        }

        public void setLatLng(LatLng latLng) {
            this.latLng = latLng;
        }

        public DateTime getArrivalTime() {
            return arrivalTime;
        }

        public void setArrivalTime(DateTime arrivalTime) {
            this.arrivalTime = arrivalTime;
        }

        public Integer getHierarchy() {
            return hierarchy;
        }

        public void setHierarchy(Integer hierarchy) {
            this.hierarchy = hierarchy;
        }

    }


    //method that sets up a array of point nodes according to the string nodes that we sent it
    public void setupPointNodes(String[] stringNodes, String startNode, String endNode) {
        int size = stringNodes.length;

        if(startNode != null)
            size++;

        if(endNode != null)
            size++;

        pointNodes = new PointNode[size];

        int i = 0;
        for(; i < stringNodes.length; i++){
            pointNodes[i] = new PointNode(stringNodes[i],false, false);
        }

        if(startNode != null)
            pointNodes[++i] = new PointNode(startNode, true, false);

        if(endNode != null)
            pointNodes[++i] = new PointNode(startNode, false, true);

    }
}
