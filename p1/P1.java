/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p1;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Edward Chiu, Inheung Ji, Brian Luo
 */
public class P1 {
    //for TCP connection

    protected SocketServer server;
    protected SocketClient client;
    protected static ConcurrentHashMap<String, Integer> localTable;

    //for UDP connection
    protected DatagramSocket dsock;
    protected DatagramPacket sPack, rPack;
    protected InetAddress initServer;
    protected InetAddress targetServer;
    protected static ConcurrentHashMap<Integer, String> udpIpTable;

    public List<File> listFiles() {
        List<File> files = new ArrayList<File>();
        try (Stream<Path> paths = Files.walk(Paths.get("pictures" + File.separator))) {
            files = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            Logger.getLogger(P1.class.getName()).log(Level.SEVERE, null, ex);
        }
        return files;
    }

    public void updateTable(File newfile) {
        List<File> files = listFiles();
        if (localTable.isEmpty() && !files.isEmpty()) {
            for (File file : files) {
                localTable.put(file.getName(), hashFile(file));
            }
        } else {
            localTable.put(newfile.getName(), hashFile(newfile));
        }
    }

    public int hashFile(File file) {
        int serverID = 0;
        String filename = file.getName();
        char[] chars = filename.toCharArray();
        for (char b : chars) {
            serverID += b;
        }
        serverID = (serverID % 4) + 1;
        return serverID;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        int informToServerID = 0;
        int clientPort = 20401;
        String requestFileName = "fanBlink.jpg";

        //open UDP socket for connecting
        String dhtServer1 = "127.0.0.1";
        UDPServer udpserver = new UDPServer(dhtServer1, 20400);

        //initialization(get ip address from each server)
        udpIpTable = udpserver.initRequestIpTable();

        //Wait a bit
        do {
            
        } while (udpIpTable.size() < 4);
        
        //open TCPServer socket for connecting
        SocketServer server = new SocketServer(20400);
        new Thread(server).start();
        server.run();
        
        //update local hash table.
        informToServerID = udpserver.hashFile(requestFileName);
        localTable.put(requestFileName, informToServerID);

        //open udp thread to listen for inbound messages
        Thread udpThread = new Thread(udpserver.UDPRunnable);
        
        //inform client information to target server.
        udpserver.queryFile(requestFileName, udpIpTable.get(informToServerID), clientPort);
        
        
        
        udpserver.sendExit(dhtServer1, clientPort);
        System.out.println("Client disconnected...");

    }

}
