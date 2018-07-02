package Servlets;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import static util.GoogleMapsApi.setUpGoogleApi;

public class OnStart implements ServletContextListener
{
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        setUpGoogleApi();
    }//end contextInitialized method


    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }//end constextDestroyed method

}
