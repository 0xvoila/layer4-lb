package com.amit;

public class Main{

    public static void main(String args[]){

        UpStreamManagement upStreamManagement = new UpStreamManagement();
        Thread upStreamManagementThread = new Thread(upStreamManagement, "upstream_management_thread");
        upStreamManagementThread.start();
        Thread loadBalancerThread = new Thread(new LoadBalancer(upStreamManagement), "load_balancer_thread");
        loadBalancerThread.start();

        Thread healthCheckThread = new Thread(new UpStreamHealthMonitor(upStreamManagement), "upstream_monitoring_thread");
        healthCheckThread.start();
    }

}



