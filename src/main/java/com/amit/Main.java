package com.amit;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Main{

    private static Logger logger = LogManager.getLogger(Main.class);
    Configurations configs = new Configurations();
    Configuration config = null;

    public static void main(String args[]) {

        Main main = new Main();
        main.startLBService();
    }

    public void startLBService(){

        try{
            this.config = configs.properties(new File("AppProperties.properties"));
        }
        catch(Exception exception){
            exception.printStackTrace();
        }

        logger.info("Starting upstream management service. You need to add upstreams manually from stdin");
        UpStreamManagement upStreamManagement = new UpStreamManagement(this.config);
        Thread upStreamManagementThread = new Thread(upStreamManagement, "upstream_management_thread");
        upStreamManagementThread.start();

        logger.info("Starting load balancer service");
        Thread loadBalancerThread = new Thread(new LoadBalancer(upStreamManagement, this.config), "load_balancer_thread");
        loadBalancerThread.start();

        logger.info("Starting health check service");
        Thread healthCheckThread = new Thread(new UpStreamHealthMonitor(upStreamManagement, this.config), "upstream_monitoring_thread");
        healthCheckThread.start();
    }

}



