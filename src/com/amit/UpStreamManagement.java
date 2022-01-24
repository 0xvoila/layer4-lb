package com.amit;

//https://stackoverflow.com/questions/13088363/how-to-wait-for-data-with-reentrantreadwritelock

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public void addUpStream(String serverAddress, int serverPort){

        try{
            writeLock.lock();
            UpStream newServer = new UpStream(serverAddress, serverPort);
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
                    if(temp.serverStatus != "CONNECTING"){
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
                if (serverURI[0].trim().equals("") || serverURI[1].trim().equals("")){
                    System.out.println("UpStream format - <UPSTREAM_IP> <UPSTREAM_PORT>");
                    continue;
                }
                addUpStream(serverURI[0], Integer.parseInt(serverURI[1]));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class UpStream{
    String serverAddress;
    int serverPort;
    String serverStatus;
    UpStream next;

    UpStream(String serverAddress, int serverPort){
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.serverStatus = "CONNECTED";
        this.next = null;
    }

    public void setServerAddress(String serverAddress ){
        this.serverAddress = serverAddress;
    }

    public void setServerStatus(String serverStatus ){
        this.serverStatus = serverStatus;
    }

    public String getServerAddress(){
        return this.serverAddress;
    }

    public int getServerPort(){
        return this.serverPort;
    }
    public String getServerStatus(){
        return this.serverStatus;
    }
}