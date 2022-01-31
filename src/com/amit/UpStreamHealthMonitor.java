package com.amit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;


public class UpStreamHealthMonitor extends Thread{

    UpStreamManagement upStreamManagement;

    private static Logger logger = LogManager.getLogger(UpStreamHealthMonitor.class);
    public UpStreamHealthMonitor(UpStreamManagement upStreamManagement){
        this.upStreamManagement = upStreamManagement;
    }
    public void run()  {

        try{
            logger.info("Upstream health monitor service started");

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
                            logger.info("Checking health of upstream " + upStream.serverPort + " " + upStream.serverStatus);
                            Boolean isConnected = clientSocket.connect(new InetSocketAddress(upStream.getServerAddress(), upStream.getServerPort()));
                            clientSocket.configureBlocking(false);

                            if(upStream.serverStatus == UpStreamStatus.DISCONNECTED){
                                upStreamManagement.updateStatus(upStream, UpStreamStatus.CONNECTED);
                            }

                        }
                        catch(Exception exception){
                            logger.warn("Upstream not connected with load balancer " + upStream.serverAddress + " " + upStream.serverPort );
                            upStreamManagement.updateStatus(upStream, UpStreamStatus.DISCONNECTED);
                        }
                        finally {
                            clientSocket.close();
                        }

                    }
                    logger.debug("Will check after 20 seconds again");
                    sleep(20000);
                }

            }
        }

        catch(Exception exception){
            exception.printStackTrace();
        }

    }
}
