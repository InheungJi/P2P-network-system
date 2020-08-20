/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dhtserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Edward Chiu, Inheung Ji, Brian Luo
 */
public class DHTSocketServer {

    /**
     * Default port number
     */
    protected int port = 20400;

    /**
     * Socket to be used for UDP inbound and outbound packets
     */
    protected DatagramSocket UDPSocket;

    /**
     * Socket listening for inbound TCP packets
     */
    protected ServerSocket TCPSocket;

    /**
     * Server ID
     */
    protected int id = 1;

    /**
     * Next Server ID in the ring network
     */
    protected int nextId;

    /**
     * Server's Address
     */
    protected InetAddress thisServer;

    /**
     * Next Server's Address in the ring network
     */
    protected InetAddress nextServer;

    /**
     * HashMap Storing <filename, IPAddress of peer that has the file>
     */
    protected ConcurrentHashMap<String, String> hashTable;

    /**
     * Number of DHTServers
     */
    protected final int SERVERS = 4; // number of DHT servers

    /**
     *
     * @param port Port number that TCP/UDP will use, default is 20400
     * @param id ServerID
     * @param thisServer Current Server's IPAddress or hostname
     * @param nextServer Next Server's IPAddress or hostname
     * @throws UnknownHostException
     */
    public DHTSocketServer(int port, int id, String thisServer, String nextServer) throws UnknownHostException {
        this.port = port;
        this.id = id;
        this.nextId = id == SERVERS ? 1 : id + 1;
        this.thisServer = InetAddress.getByName(thisServer);
        this.nextServer = InetAddress.getByName(nextServer);
        this.hashTable = new ConcurrentHashMap<String, String>();
        //this.runningThread = Thread.currentThread();
    }

    /**
     *
     * @return serverID
     */
    public int getID() {
        return this.id;
    }

    /**
     *
     * @return next serverID in the ring
     */
    public int getNextID() {
        return this.nextId;
    }

    /**
     *
     * @return next serverIP
     */
    public InetAddress getNextServerIP() {
        return this.nextServer;
    }
    
    /**
     *
     * @return hashMap contain the filename and IP of file location
     */
    public ConcurrentHashMap getHashTable() {
        return this.hashTable;
    }

    private void sendTCPMessage(String message) {
        boolean sent = false;
        try {
            // For testing on same machine
            //Socket connectToNext = new Socket(this.nextServer.getHostAddress(), this.port + 1);
            Socket connectToNext = new Socket(this.nextServer.getHostAddress(), this.port);
            System.out.println(this.nextServer.getHostAddress());
            System.out.println(InetAddress.getLoopbackAddress());
            System.out.println("Message sending.. " + message);
            OutputStream out = connectToNext.getOutputStream();
            out.write(message.getBytes("UTF-8"));
            connectToNext.close();
            sent = true;
        } catch (UnknownHostException e) {
            System.out.println("Can not connect to unknown next TCP socket.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect TCP for connection.", e);
        } finally {
            //return sent;
        }
    }

    private boolean sendUDPMessage(String message, InetAddress add) throws UnknownHostException {
        boolean sent = false;
        try {
            byte[] sendData = new byte[1024];
            sendData = message.getBytes("UTF-8");
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, add, this.port);
            UDPSocket.send(sendPacket);
            sent = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to send UDP message.", e);
        } finally {
            return sent;
        }
    }

    // Runnable thread to listen for inbound UDP Packets and to respond appropriately
    Runnable UDPRunnable = new Runnable() {

        public void run() {
            /*synchronized (this) {
            this.runningThread = Thread.currentThread();
        }*/
            openServerUDPSocket();
            //System.out.println(UDPSocket.getLocalAddress().toString());

            byte[] receiveData = new byte[1024];

            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    UDPSocket.receive(receivePacket);
                    //System.out.println(receivePacket.getAddress().toString());
                    String message = receivePacket.getData().toString();
                    InetAddress add = receivePacket.getAddress();
                    System.out.print("Received: " + message);
                    if (message.equalsIgnoreCase("Request for IP address") && id == 1) {
                        sendUDPMessage((id + " " + thisServer.toString()), add);
                        sendTCPMessage("GET IP/" + add);
                        System.out.println("ID " + id + " " + thisServer.toString());
                    }
                    else if (message.equalsIgnoreCase("EXIT")) {
                        //Exit
                        if (hashTable.containsValue(add)) {
                            for (Enumeration<String> e = hashTable.keys(); e.hasMoreElements();) {
                                hashTable.remove(e.nextElement(), add);
                            }
                            System.out.println("EXIT Server " + id + " " + hashTable.size());
                        }
                    }
                    else {
                        //filename + " " + ip
                        String[] req = message.split(" ");
                        hashTable.putIfAbsent(req[0], req[1]);
                        System.out.println("Hashtable: " + hashTable.keys().toString());
                    }
                    
                } catch (IOException e) {
                    throw new RuntimeException("Error receive packet.", e);
                }
            }
        }
    };

    // Runnable TCP Thread to listen for inbound packets and to respond
    Runnable TCPRunnable = new Runnable() {
        @Override
        public void run() {
            /*synchronized (this) {
            runningThread = Thread.currentThread();
        }*/
            openServerTCPSocket();
            sendTCPMessage("Hello Server " + nextId);

            while (true) {
                Socket previousServer = null;
                try {
                    previousServer = TCPSocket.accept();

                    InputStream is = previousServer.getInputStream();
                    BufferedReader input = new BufferedReader(
                            new InputStreamReader(is));
                    OutputStream output = previousServer.getOutputStream();

                    String message;
                    message = input.readLine();

                    // on init, propogate message to all servers so they can send their IP to peer
                    if (message.startsWith("GET IP/")) {
                        String[] req = message.split(" ");
                        InetAddress add = InetAddress.getByName(req[1].substring(3));
                        sendUDPMessage((id + " " + thisServer.toString()), add);
                        output.write(("ACK " + message).getBytes("UTF-8"));
                        
                        if (id != SERVERS) {
                            sendTCPMessage(message);
                        }
                    }
                    // query of file
                    else if (message.startsWith("GET File/")) {
                        String[] req = message.split(" ");
                        InetAddress add = InetAddress.getByName(req[1].substring(5));
                        String query = req[2];
                        output.write(("ACK " + message).getBytes("UTF-8"));
                        if (hashTable.contains(query)) {
                            String response = hashTable.get(query);
                            sendUDPMessage("FOUND File/" + query + " " + response, add);
                        }
                        else {
                            if (id != SERVERS) {
                                sendTCPMessage(message);
                            }
                            else {
                                sendUDPMessage("NOTFOUND File/" + query + " " + add, add);
                            }
                        }
                    }
                    output.flush();
                    previousServer.close();
                } catch (SocketException e) {
                    System.out.println("Disconnected socket thread in TCPRunnable");
                    //e.printStackTrace();
                } catch (IOException e) {
                    System.out.println("TCPRunnable did not process request.");
                    e.printStackTrace();
                } finally {
                    System.out.println("End of TCPRunnable thread");
                }
            }
        }

    };

    /**
     *
     * @param table
     */
    public void setTable(ConcurrentHashMap table) {
        this.hashTable = table;
    }

    private void openServerUDPSocket() {
        try {
            this.UDPSocket = new DatagramSocket(this.port);
        } catch (IOException e) {
            throw new RuntimeException("Can not open UDP port " + this.port, e);
        }
    }

    private void openServerTCPSocket() {
        try {
            this.TCPSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            throw new RuntimeException("Can not open TCP port " + this.port, e);
        }
    }
}
