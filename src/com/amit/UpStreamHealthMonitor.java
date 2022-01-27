package com.amit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.nio.channels.Selector;

public class UpStreamHealthMonitor extends Thread{

    UpStreamManagement upStreamManagement;

    public UpStreamHealthMonitor(UpStreamManagement upStreamManagement){
        this.upStreamManagement = upStreamManagement;
    }
    public void run()  {

        try{
            System.out.println("Monitoring upstreams");

            while(true){

                List<UpStream> upStreamList = upStreamManagement.getUpStreamList();
                upStreamManagement.display();

                if(upStreamList.size() == 0 ){
                    try {
                        sleep(20000);
                    }
                    catch(Exception exception){
                        exception.printStackTrace();
                    }
                }
                else{
                    SocketChannel clientSocket = null;

                    for (UpStream upStream : upStreamList){
                        try{
                            clientSocket = SocketChannel.open();
                            System.out.println("Monitoring upstream " + upStream.serverPort + " " + upStream.serverStatus);
                            Boolean isConnected = clientSocket.connect(new InetSocketAddress(upStream.getServerAddress(), upStream.getServerPort()));
                            clientSocket.configureBlocking(false);

                        }
                        catch(Exception exception){
                            System.out.println("Not connected " + upStream.serverAddress + " " + upStream.serverPort );
                            upStreamManagement.updateStatus(upStream, UpStreamStatus.DISCONNECTED);
                        }
                        finally {
                            clientSocket.close();
                        }

                    }
                    sleep(20000);
                }

            }
        }

        catch(Exception exception){
            exception.printStackTrace();
        }



    }
}
