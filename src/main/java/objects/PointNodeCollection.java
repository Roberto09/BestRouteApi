package objects;

import com.google.maps.model.LatLng;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;


//PointNodeCollection class that contains an array with al the point nodes and some common variables all the points in the array share.
//This is done this way to avoid the use of static variables in the PointNode class and therefore avoid multi threading problems whenevever more than one
//servlet instance is created makes changes to those static variables.
public class PointNodeCollection {
    //public variabes pertaining to all the point nodes
    public boolean timeParam = false, hierarchyParam = false, packagesParam = false;
    public String timeZone = "GMT";
    public boolean routeHasStartNode = false; //if there's not one specified it is the first node in the regular points list
    public boolean routeHasEndNode = false; //if there's not one specified it is the first node in the regular points list
    public int routeStartPosition = 0, routeEndPosition = 0; //position in PointNodes array of the start and end point of the route(s)
    public Long routesMaxTime = Long.MAX_VALUE;
    //array of point nodes
    public PointNode[] pointNodes;


    public PointNodeCollection(boolean timeParam, boolean hierarchyParam, boolean packagesParam, String timeZone, boolean routeHasStartNode, boolean routeHasEndNode, Long routesMaxTime) {
        this.timeParam = timeParam;
        this.hierarchyParam = hierarchyParam;
        this.packagesParam = packagesParam;
        this.routeHasStartNode = routeHasStartNode;
        this.routeHasEndNode = routeHasEndNode;
        if(timeZone != null)
            this.timeZone = timeZone;
        if(routesMaxTime != null)
            this.routesMaxTime = routesMaxTime;
    }

    public class PointNode{

        //Initialize our variables with default values since here
        private String latLngStr = null; //latitude and longitude in string format
        private LatLng latLng = null; //latitude and longitude of the point
        private DateTime arrivalTime = null; //time at which it should arrive
        private Integer hierarchy = null; //hierarchy of the visit
        private boolean isStart = false; //true if the node is the start of the route
        private boolean isEnd = false; // true if the node is the end of the route
        private boolean picksUpPackage = false; //true if the transport has to pick a package in that poisition, false if it has to deliver one
        private Integer packageWeight = null; //weight of the package that needs to be picked up or delivered


        //Constructor used for GET requests
        // Receives string of type latitude,longitude|formatedDatetime|hierarchy|packageInfo and also 2 boolean variables which determine
        // whether the point node is the start, the end or it's between the route.
        // format of rawString: nn.nnn,nnn.nnn|dd/MM/yyyy HH:mm:ss|n|n,tag
        // example: 24.872,-100.415|04/06/2018 04:27:32|3|42,PU
        public PointNode(String rawString, boolean start, boolean end){

            //set up of node start and node end (if there is one)
            this.isStart = start;
            this.isEnd = end;

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
            this.latLngStr = rawString.substring(0, paramDivisor);
            this.latLng = new LatLng(lat, lng);

            //if there's a time param we set it up
            if(timeParam){
                //then we get the formated Datetime
                rawString = rawString.substring(paramDivisor + 1, rawString.length());

                if(hierarchyParam || packagesParam)
                    paramDivisor = rawString.indexOf('|');
                else
                    paramDivisor = rawString.length();

                String formatedDatetime = rawString.substring(0, paramDivisor); //we add the timezone
                DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
                this.arrivalTime = formatter.parseDateTime(formatedDatetime);
            }

            //if there's a hierarchy param we set it up
            if(hierarchyParam){
                //then we set up the hierarchy
                rawString = rawString.substring(paramDivisor + 1, rawString.length());

                if(packagesParam)
                    paramDivisor = rawString.indexOf('|');
                else
                    paramDivisor = rawString.length();

                this.hierarchy = Integer.parseInt(rawString.substring(0, paramDivisor));
                System.out.print("Hierarchy: " + hierarchy);
            }

            //if there's a packagesParam we set it up
            if(packagesParam){
                //finally we setup the package info
                String packageInfo = rawString.substring(paramDivisor + 1, rawString.length());
                System.out.println("package info" + packageInfo);
                commaPos = packageInfo.indexOf(',');
                this.packageWeight = Integer.parseInt(packageInfo.substring(0, commaPos));

                //see if we can find 'PU' here
                if(packageInfo.contains("PU")){
                    this.picksUpPackage = true;
                    System.out.println("PU");
                }
                else
                    this.picksUpPackage = false;
                    System.out.println("Not PU");

            }

        }

        //Constructor set for POST requests
        //Receives a JsonObject which has all the point information such as latitude and longitude, arrival time, hierarchy and package info
        public PointNode(JSONObject nodeJson, boolean start, boolean end) {
            //set up of node start and node end (if there is one)
            this.isStart = start;
            this.isEnd = end;

            //setup latitude longitude with latLngString
            latLngStr = nodeJson.getString("LatLng");//aki me kede truena si es minusculas
            int commaPos = latLngStr.indexOf(',');
            float lat = Float.parseFloat(latLngStr.substring(0, commaPos));
            float lng = Float.parseFloat(latLngStr.substring(commaPos + 1, latLngStr.length()));
            latLng = new LatLng(lat, lng);


            //if there's a time parameter
            //format is the same dd/MM/yyyy hh:mm:ss
            if(timeParam) {
                String time = nodeJson.getString("ArrivalTime");
                DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
                this.arrivalTime = formatter.parseDateTime(time);
            }

            //if there's a hierarchy param
            if(hierarchyParam) {
                this.hierarchy = nodeJson.getInt("Hierarchy");
            }
            //if there's a package param
            //format is the same n,tag
            if(packagesParam){
                String packageInfo = nodeJson.getString("PackageInfo");
                commaPos = packageInfo.indexOf(',');
                packageWeight = Integer.parseInt(packageInfo.substring(0, commaPos));

                //see if we can find 'PU' here
                if(packageInfo.contains("PU")){
                    this.picksUpPackage = true;
                    System.out.println("PU");
                }
                else
                    this.picksUpPackage = false;
                System.out.println("Not PU");
            }
        }

        // getter and setter methods
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

        public boolean isStart() {
            return isStart;
        }

        public void setStart(boolean start) {
            isStart = start;
        }

        public boolean isEnd() {
            return isEnd;
        }

        public void setEnd(boolean end) {
            isEnd = end;
        }

        public boolean isPicksUpPackage() {
            return picksUpPackage;
        }

        public void setPicksUpPackage(boolean picksUpPackage) {
            this.picksUpPackage = picksUpPackage;
        }

        public int getPackageWeight() {
            return packageWeight;
        }

        public void setPackageWeight(int packageWeight) {
            this.packageWeight = packageWeight;
        }
    }



    //method that sets up a array of point nodes according to the string nodes that we sent it in GET requests
    public void setupPointNodes(String[] stringNodes, String startNode, String endNode) {
        int size = stringNodes.length;

        if(routeHasStartNode){
            System.out.println("start node not null");
            size++;
        }

        if(routeHasEndNode)
            size++;

        pointNodes = new PointNode[size];

        int i = 0;
        //Note, here we are saving the start node int the position #0 of the array
        //this is important to calculate the shortest path since our method will use this point as start point
        if(routeHasStartNode) {
            pointNodes[i] = new PointNode(startNode, true, false);
            routeStartPosition = i++;
            if(!routeHasEndNode)
                routeEndPosition = 1;
        }

        //Note, here we are saving the start node int the position #1 of the array
        //this is important to calculate the shortest path since our method will use this point as start point
        if(routeHasEndNode) {
            pointNodes[i] = new PointNode(endNode, false, true);
            routeEndPosition = i++;
            if(!routeHasStartNode)
                routeStartPosition = 1;
        }

        for(int j = 0; j < stringNodes.length; j++, i++){
            pointNodes[i] = new PointNode(stringNodes[j],false, false);
        }
    }

    //method that sets up a array of point nodes according to the json nodes that we sent it in POST requests
    public void setUpPointNodesJson(JSONArray nodes, JSONObject startNode, JSONObject endNode){
        int size = nodes.length();

        if(routeHasStartNode)
            size ++;
        if(routeHasEndNode)
            size ++;

        pointNodes = new PointNode[size];

        int i = 0;
        //Note, here we are saving the start node in the position #0 of the array
        //this is important to calculate the shortest path since our method will use this point as start point
        //String latLngStr, String arrivalTime, Integer hierarchy, boolean isStart, boolean isEnd, String packageStr
        if(routeHasStartNode){
            pointNodes[i] = new PointNode(startNode, true, false);
            routeStartPosition = i++;
            if(!routeHasEndNode)
                routeEndPosition = 1;
        }
        //Note, here we are saving the start node int the position #1 of the array
        //this is important to calculate the shortest path since our method will use this point as start point
        if(routeHasEndNode) {
            pointNodes[i] = new PointNode(endNode, false, true);
            routeEndPosition = i++;
            if(!routeHasStartNode)
                routeStartPosition = 1;
        }

        for(int j = 0; j < nodes.length(); j++, i++){
            pointNodes[i] = new PointNode(nodes.getJSONObject(j), false, false);
        }
    }
}
