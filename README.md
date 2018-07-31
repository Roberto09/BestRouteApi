# BestRouteApi (beta)
Routing Api made with Java, Servlets, MapsApi and GoogleOrtools Library

BesRouteApi is an API done in Java with servlets, GoogleOrtools library and GoogleMaps Distance Matrix API that allows you to make efficient routes.

The API works by sending it a given group of coordinates through a GET or POST request along with some other optional parameters such as:

Packaging information for each node
-Hierarchy of visit for each node
-Number of vehicles
-Max load the vehicles can handle
-Time window Parameters

*Currently working on wiki to add examples and tutorials on how to use the api properly

This code is intended to be pulled and deployed on heroku or any server, however, the link for testing purposes is the following:

Try the following link on postman or any other client platform capable of receiving Json format responses -> https://bestrouteapi.herokuapp.com/EntryPointOne/packages/hierarchy?point=25.644726,-100.291025%7C1%7C2,PU&point=25.699910,-100.362560%7C2%7C2,DE&point=25.657399,%20-100.256759%7C1%7C2,PU&vehicleCapacity=10&numberOfVehicles=2&point=25.669122,%20-100.284095%7C3%7C2,DE

The code is all documented in case you have a doubt of how does it work. If you notice anything strange or that can improve feel free to email me at roberto.gt1509@gmail.com.
