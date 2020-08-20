/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dhtserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Edward Chiu, Inheung Ji, Brian Luo
 */
public class DHTServer {

    private int port;

    /**
     *
     */
    protected ConcurrentHashMap record;
    
    /**
     *
     * @param port
     */
    public DHTServer(int port) {
        this.port = port;
        this.record = new ConcurrentHashMap();
    }
    /**
     * @param args the command line arguments
     * @throws java.net.UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException {
        String thisServerIP = "127.0.0.1";
        String nextServerIP = "127.0.0.1";
        DHTSocketServer s1 = new DHTSocketServer(20400, 1, thisServerIP, nextServerIP);
        DHTSocketServer s2 = new DHTSocketServer(20401, 2, thisServerIP, nextServerIP);
        DHTSocketServer s3 = new DHTSocketServer(20402, 3, thisServerIP, nextServerIP);
        DHTSocketServer s4 = new DHTSocketServer(20403, 4, thisServerIP, nextServerIP);
        
        System.out.println("LocalHost: " + InetAddress.getLoopbackAddress());
        
        // Start the tcpThread that listens for incoming messages
        Thread tcpThread = new Thread(s1.TCPRunnable);
        // Start the udpThread that listens for incoming messages
        Thread udpThread = new Thread(s1.UDPRunnable);
        tcpThread.start();
        udpThread.start();
                
    }
    
}
