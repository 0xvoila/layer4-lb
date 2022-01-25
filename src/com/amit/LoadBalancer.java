package com.amit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;



public class LoadBalancer extends Thread{
    static int numberOfConnection = 0;
    static int numberOfBackStreamConnections = 0;
    UpStreamManagement upStreamManagement;
    private static Logger logger = LogManager.getLogger(LoadBalancer.class);

    LoadBalancer(UpStreamManagement upStreamManagement){
        this.upStreamManagement = upStreamManagement;
    }

    public void run() {
        try {
            Selector selector = Selector.open();
            ServerSocketChannel serverSocket = ServerSocketChannel.open();

            serverSocket.bind(new InetSocketAddress("localhost", 10000));
            serverSocket.configureBlocking(false);

            HashMap<String, Object> attr = new HashMap<>();
            attr.put("channel_type", "server");
            attr.put("back_connection", "");
            attr.put("last_used_time", java.time.Instant.now().getEpochSecond());

            SelectionKey selectionKey = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            selectionKey.attach(attr);


            while (true) {

                selector.select();
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectionKeySet.iterator();
                logger.info("Iterator set count is " + selectionKeySet.size());
                while (iter.hasNext()) {

                    selectionKey = iter.next();
                    iter.remove();
                    String channelType = "";

                    logger.info("Notification from " + selectionKey.channel());
                    logger.info("Number of connections accepted are " + numberOfConnection);
                    logger.info("Number of backstream connections accepted are " + numberOfBackStreamConnections);

                    if (selectionKey.isValid() && selectionKey.attachment() != null) {
                        attr = (HashMap) selectionKey.attachment();
                        channelType = (String) attr.get("channel_type");
                        logger.info("attachment is " + attr);
                        logger.info("Channel type is " + channelType);

                    }

                    if (selectionKey.isValid() && selectionKey.isAcceptable() && channelType.equals("server")) {
                        logger.info("Server Connection is accepted");

                        SocketChannel connection = serverSocket.accept();
                        numberOfConnection = numberOfConnection + 1;
                        connection.configureBlocking(false);

                        HashMap<String, Object> x = new HashMap<>();
                        x.put("channel_type", "server");
                        x.put("back_connection", "");
                        x.put("last_used_time", java.time.Instant.now().getEpochSecond());


                        SelectionKey selectionKey1 = connection.register(selector, SelectionKey.OP_READ);
                        selectionKey1.attach(x);
                    }


                    if (selectionKey.isValid() && selectionKey.isReadable() && channelType.equals("server")) {

                        logger.info("Server Channel is readable");
                        SocketChannel connection = (SocketChannel) selectionKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);

                        try {
                            int bytesRead;
                            bytesRead = connection.read(buffer);
                            if (connection.read(buffer) < 0) {
                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();

                                long lastUsed = (long) yy.get("last_used_time");
                                long now = java.time.Instant.now().getEpochSecond();

                                long timeElapsed = now - lastUsed;

                                if (timeElapsed > 15) {
                                    logger.debug("15 seconds elapsed without any data transfer, terminating downstream connection");
                                    connection.close();
                                }
                                else{
                                    logger.debug("Client has stopped sending the data but not closed the connection");
                                }
                                
                                continue;
                            } else {

                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();
                                yy.put("last_used_time", java.time.Instant.now().getEpochSecond());
                                selectionKey.attach(yy);


                                //After reading the request from downstream, make a connection with upstream

                                HashMap<String, Object> attrClient = new HashMap<>();
                                attrClient.put("channel_type", "client");
                                attrClient.put("back_connection", connection);
                                attrClient.put("last_used_time", java.time.Instant.now().getEpochSecond());

                                SocketChannel clientSocket = null;
                                UpStream upStream = this.upStreamManagement.getNextUpStream();
                                try{

                                    //Here Get the upstream from UpStreamManagementService
                                    if(upStream == null){
                                        logger.warn("No UpStream is available");
                                        continue;
                                    }

                                    logger.debug("Upstream selected is " + upStream.serverAddress + ":" + upStream.serverPort);
                                    clientSocket = SocketChannel.open();
                                    clientSocket.connect(new InetSocketAddress(upStream.getServerAddress(), upStream.getServerPort()));
                                    clientSocket.configureBlocking(false);
                                }
                                catch(Exception exception){
                                    clientSocket.close();
                                    connection.close();
                                    logger.fatal("Unable to reach upstream server");
                                    logger.warn("Closing server socket connection");
                                    logger.fatal(upStream.serverAddress + " " + upStream.serverPort);
                                    continue;
                                }

                                numberOfBackStreamConnections = numberOfConnection + 1;
                                logger.debug("Apache socket is " + clientSocket);
                                SelectionKey selectionKey2 = clientSocket.register(selector, SelectionKey.OP_READ);
                                selectionKey2.attach(attrClient);

                                logger.info("Downstream connection attached " + attrClient.get("back_connection"));

                                while (bytesRead > 0) {

                                    buffer.flip();
                                    clientSocket.write(buffer);
                                    buffer.clear();
                                    bytesRead = connection.read(buffer);

                                }

                            }
                        } catch (Exception e) {
                            connection.close();
                            selectionKey.cancel();
                            e.printStackTrace();
                        }

                    }

                    if (selectionKey.isValid() && selectionKey.isReadable() && channelType.equals("client")) {

                        logger.info("Client Channel is readable");
                        SocketChannel connection = (SocketChannel) selectionKey.channel();
                        logger.info("Apache socket for readable " + connection);
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        logger.info("Data from apache is " + new String(buffer.array()));

                        try {

                            int bytesRead;
                            bytesRead = connection.read(buffer);
                            if (bytesRead < 0) {

                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();

                                long lastUsed = (long) yy.get("last_used_time");
                                long now = java.time.Instant.now().getEpochSecond();

                                long timeElapsed = now - lastUsed;

                                if (timeElapsed > 15) {
                                    logger.debug("15 seconds elapsed without any data transfer, terminating upstream connection");
                                    connection.close();
                                }


                                logger.warn("Upstream has stopped sending the data but has not closed the connection");
                                continue;

                            } else {

                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();
                                yy.put("last_used_time", java.time.Instant.now().getEpochSecond());
                                selectionKey.attach(yy);

                                SocketChannel conn = null;
                                try {

                                    if (selectionKey.attachment() != null) {
                                        attr = (HashMap<String, Object>) selectionKey.attachment();
                                        selectionKey.attach(null);
                                        conn = (SocketChannel) attr.get("back_connection");

                                        while (bytesRead > 0) {

                                            buffer.flip();
                                            conn.write(buffer);
                                            buffer.clear();
                                            bytesRead = connection.read(buffer);

                                        }


                                        logger.info("Sending data back to " + conn);


                                    } else {
                                        logger.debug("No one to send data back");
                                    }
                                } catch (Exception exception) {
                                    logger.error("Error while writing to client socket" + conn);
                                    exception.printStackTrace();
                                    conn.close();
                                }

                            }
                            connection.close();
                        } catch (Exception e) {
                            logger.error("Exception while reading from apache connection" + connection);
                            e.printStackTrace();
                            connection.close();

                            logger.error("If apache ReSET the connection, then ELB close the client connection");
                            if (selectionKey.attachment() != null) {
                                attr = (HashMap<String, Object>) selectionKey.attachment();
                                selectionKey.attach(null);
                                SocketChannel conn = (SocketChannel) attr.get("back_connection");
                                conn.close();
                            }

                        }

                    }


                }
            }

        } catch (Exception exception) {
            logger.fatal("Exception has occured");
            exception.printStackTrace();
            logger.fatal(exception.getMessage());
        }
    }
}