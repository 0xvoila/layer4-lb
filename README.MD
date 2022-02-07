# Arogya - Layer 4 Load Balancer in Java

Argoya is the layer 4 load balancer developed in Java. Arogya is non-blocking single threaded load balancer.
Arogya has number of features incorporated based inspired from Elastic Load Balancer of AWS.


##Features of the load balancer 
1. Non-blocking, single threaded layer 4 load balancer
2. Upstreams can be registered on a fly without reloading the load balancer
3. Load Balancer automatically checks the health of the up streams at a particular interval of time
4. Remove the upstream from load balancer if upstream is unhealthy
5. Route downstream requests based on Round robin algorithm 
6. Handles upstream and downstream TCP RESET exceptions
7. Automatically disconnect downstream idle connections after 15 seconds of idle time

## How to deploy 

1. Download the repo 
2. Change directory to `cd later4_lb`
3. Set configuration in `AppProperties.properties` file
4. Run maven clean and package command `mvn clean package`
5. Run load balancer by `java -jar target/Layer4LB.jar`

