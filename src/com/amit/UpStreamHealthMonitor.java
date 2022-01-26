package com.amit;

import java.util.List;

public class UpStreamHealthMonitor {

    UpStreamManagement upStreamManagement;

    public UpStreamHealthMonitor(UpStreamManagement upStreamManagement){
        this.upStreamManagement = upStreamManagement;
    }
    public void run(){

        List<UpStream> upStreamList = upStreamManagement.getUpStreamList();


    }
}
