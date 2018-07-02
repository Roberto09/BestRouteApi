package Servlets;

import com.google.maps.model.DistanceMatrix;
import objects.Node;
import objects.PointNode;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import util.GoogleMapsApi;
import util.ShortestPath;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class EntryPointOne extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        //setup the boolean variables regarding the request path
        boolean pointTimeParameters = false;
        boolean pointHierarchyParameters = false;
        boolean pointPackageParameters = false;

        //setup timezone param

        //setup the pathParams info
        String info = request.getPathInfo();
        if(info != null){
            String[] pathParams = info.split("/");

            //checking for patameters *order doesn't matter at all for this*
            for(int i = 0; i < pathParams.length; i++){
                if (pathParams[i] == "time"){
                    pointTimeParameters = true;
                }
                if (pathParams[i].equals("hierarchy")) {
                    pointHierarchyParameters = true;
                }
                if (pathParams[i].equals("packages")) {
                    pointPackageParameters = true;
                }
            }
        }

        //getting points
        String points[] = request.getParameterValues("point");
        String startNode = request.getParameter("startPoint");
        String endNode = request.getParameter("endPoint");

        // creating  arraylist from points
        PointNode[] pointNodes = PointNode.setupPointNodes(points, startNode, endNode,pointTimeParameters, pointHierarchyParameters, pointPackageParameters, "PST");
        DistanceMatrix distanceMatrix = GoogleMapsApi.getDistances(pointNodes);
        if (distanceMatrix != null) {
            ShortestPath.getShortestPath(distanceMatrix, points);
        }

        //creating our output
        PrintWriter out = response.getWriter();
        out.print("Todo bien");

        String formatedDatetime = "01/01/2018 10:00:00 PST";
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss z");
        DateTime x = formatter.parseDateTime(formatedDatetime);

        String formatedDatetime2 = "01/01/2018 10:25:45 PST";
        DateTime x2 = formatter.parseDateTime(formatedDatetime2);

        int minutes = Minutes.minutesBetween(x, x2).getMinutes();
        System.out.println(minutes);

        //--------Test-----------

        Node node = new Node(2);
        node.x += Integer.parseInt(request.getParameter("sum"));


        Node.TestPNode x = node.new TestPNode("test", 4);


    }
}
