package com.amit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main{

    private static Logger logger = LogManager.getLogger(Main.class);

    public static void main(String args[]){

        logger.info("Starting upstream management service. You need to add upstreams manually from stdin");
        UpStreamManagement upStreamManagement = new UpStreamManagement();
        Thread upStreamManagementThread = new Thread(upStreamManagement, "upstream_management_thread");
        upStreamManagementThread.start();

        logger.info("Starting load balancer service");
        Thread loadBalancerThread = new Thread(new LoadBalancer(upStreamManagement), "load_balancer_thread");
        loadBalancerThread.start();

        logger.info("Starting health check service");
        Thread healthCheckThread = new Thread(new UpStreamHealthMonitor(upStreamManagement), "upstream_monitoring_thread");
        healthCheckThread.start();
    }

}



