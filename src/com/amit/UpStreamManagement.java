package com.amit;

//https://stackoverflow.com/questions/13088363/how-to-wait-for-data-with-reentrantreadwritelock

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;



public class UpStreamManagement extends Thread{

    public static UpStream upStreamFront = null;
    public static UpStream upStreamRear = null;

    ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

    Condition  upStreamEmptyCondition = writeLock.newCondition();

    public void addUpStream(String serverAddress, int serverPort, String healthCheckUrl){

        try{
            writeLock.lock();
            UpStream newServer = new UpStream(serverAddress, serverPort, healthCheckUrl);
            if(upStreamFront == null){
                upStreamFront = newServer;
                upStreamRear = newServer;
                newServer.next = upStreamFront;

                upStreamEmptyCondition.signalAll();
            }

            else{
                UpStream temp = upStreamRear;
                upStreamRear = newServer;
                temp.next = upStreamRear;
                upStreamRear.next = upStreamFront;
            }
        }
        finally {
            writeLock.unlock();
        }
    }

    public void deleteUpStream(String serverAddress ){


//        writeLock.lock();
//        UpStream temp = upStreamFront;
//        do{
//            if(temp.serverAddress == serverAddress){
//                temp.serverStatus = "DISCONNECTED";
//                break;
//            }
//            else{
//                temp = temp.next;
//            }
//        }while(temp == upStreamRear.next);
//
//        notifyAll();
//        writeLock.unlock();
    }

    public void display(){

        UpStream temp = upStreamFront;
        do{
            System.out.println(temp.serverAddress);
            temp = temp.next;
        }while ( temp != upStreamRear.next);
    }

    public List<UpStream> getUpStreamList(){

        List<UpStream> upStreamList = null;

        try{
            readLock.lock();

            UpStream temp = upStreamFront;

            do{
                upStreamList.add(temp);
                temp = temp.next;
            }while(temp != upStreamRear);
        }
        finally {
            readLock.unlock();
        }

        return upStreamList;
    }

    public UpStream getNextUpStream() throws InterruptedException {

        UpStream nextUpStream = null;
        readLock.lock();

        try {
            if (upStreamFront == null) {
                readLock.unlock();
                System.out.println("Lock upgrading - Releasing read lock and upgrading write lock");
                writeLock.lock();

                try {
                    while (upStreamFront == null) {
                        System.out.println("No backend stream is present, going in idle state");
                        upStreamEmptyCondition.await();
                    }
                    System.out.println("Thread has waken up");
                } finally {
                    writeLock.unlock();
                    readLock.lock();
                }
            }   // If close

                UpStream temp = upStreamFront;

                do{
                    if(temp.serverStatus == UpStreamStatus.CONNECTED){
                        nextUpStream = temp;
                        upStreamFront = temp.next;
                        upStreamRear = upStreamRear.next;
                        break;
                    }
                    else{
                        temp = temp.next;
                    }
                }while(temp != upStreamRear.next);

        }  // Out try is close

        finally {
            readLock.unlock();
        }

        return nextUpStream;
    }


    public void run(){

        while (true) {

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in));

            // Reading data using readLine
            try {
                String[] serverURI = reader.readLine().split(" ");
                if (serverURI[0].trim().equals("") || serverURI[1].trim().equals("") || serverURI[2].trim().equals("")){
                    System.out.println("UpStream format - <UPSTREAM_IP> <UPSTREAM_PORT> <HEALTH_CHECK_URL>");
                    continue;
                }
                addUpStream(serverURI[0], Integer.parseInt(serverURI[1]), serverURI[2]);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class UpStream{
    String serverAddress;
    int serverPort;
    UpStreamStatus serverStatus;
    UpStream next;
    String healthCheckUrl;

    UpStream(String serverAddress, int serverPort, String healthCheckUrl){
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.serverStatus = UpStreamStatus.CONNECTED;
        this.healthCheckUrl = healthCheckUrl;
        this.next = null;
    }

    public void setServerAddress(String serverAddress ){
        this.serverAddress = serverAddress;
    }

    public void setServerStatus(UpStreamStatus serverStatus ){
        this.serverStatus = serverStatus;
    }

    public String getServerAddress(){
        return this.serverAddress;
    }

    public int getServerPort(){
        return this.serverPort;
    }

    public UpStreamStatus getServerStatus(){
        return this.serverStatus;
    }

    public String getHealthCheckUrl(){
        return this.healthCheckUrl;
    }

    public void setHealthCheckUrl(String healthCheckUrl){
        this.healthCheckUrl = healthCheckUrl;
    }
}