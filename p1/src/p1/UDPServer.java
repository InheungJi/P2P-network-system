package p1;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UDP Socket to handle inbound and outbound messages for Peer
 * @author hidden_ninja
 */
public class UDPServer {

    DatagramSocket dsock;
    DatagramPacket sPack, rPack;
    InetAddress initServer;
    InetAddress targetServer;
    //protected ConcurrentHashMap localTable;
    int port = 20401;

    /**
     *
     */
    protected ConcurrentHashMap<Integer, String> ipTable;

    /**
     *
     * @param ip
     * @param port
     */
    public UDPServer(String ip, int port) {
        try {
            initServer = InetAddress.getByName(ip);
            this.port = port;
            this.dsock = new DatagramSocket();
            this.ipTable = new ConcurrentHashMap<Integer, String>();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     *
     * @return IP table containing key ServerID and value IPAddress
     */
    public ConcurrentHashMap<Integer, String> getIpTable() {
        return ipTable;
    }

    /**
     *
     * @return return IP table containing key ServerID and value IPAddress after init
     */
    public ConcurrentHashMap<Integer, String> initRequestIpTable() {

        try {
            //BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String strOut = "Request for IP address";
            //while((strOut = br.readLine()) != null) {

            sPack = new DatagramPacket(strOut.getBytes(), strOut.getBytes().length, initServer, port);
            dsock.send(sPack);

            while (ipTable.size() < 4) {

                byte[] buffer = new byte[1024];
                String[] infoFromServer;
                rPack = new DatagramPacket(buffer, buffer.length);
                dsock.receive(rPack);
                String strIn = new String(rPack.getData(), 0, rPack.getData().length);
                infoFromServer = strIn.split(" ");
                ipTable.putIfAbsent(Integer.parseInt(infoFromServer[0]), infoFromServer[1]);

            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return ipTable;

    }
//	public ConcurrentHashMap<Integer, String> makeHashTable(ConcurrentHashMap<Integer,String> ipTable){
//		
//		for(int i = 0; i < ipTable.size(); i++) {
//			localTable.put(i,ipTable.get(i));
//		}
//		return localTable;
//	}

    /**
     * Send exit message to DHT server so client can exit safely
     * @param serverIp Server's IPAddress of Hostname
     * @param port 
     */
    public void sendExit(String serverIp, int port) {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            String strOut = "EXIT";
            targetServer = InetAddress.getByName(serverIp);
            sPack = new DatagramPacket(strOut.getBytes(), strOut.getBytes().length, targetServer, port);
            dsock.send(sPack);
        } catch (Exception e) {
            System.out.println(e);
        }

    }
    
    /**
     * Query for file in DHTServer
     * @param fileName name of file to be queried
     * @param serverIp Server 1 IPAddress
     * @param port Server1 Port
     */
    public void queryFile(String fileName, String serverIp, int port) {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            String strOut = fileName + " " + ip;
            targetServer = InetAddress.getByName(serverIp);
            sPack = new DatagramPacket(strOut.getBytes(), strOut.getBytes().length, targetServer, port);
            dsock.send(sPack);
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    /**
     * Hash function with input of filename to get a number that represents serverID
     * @param fileName
     * @return
     */
    public int hashFile(String fileName) {
        int serverID = 0;
        char[] chars = fileName.toCharArray();
        for (char b : chars) {
            serverID += b;
        }
        serverID = (serverID % 4) + 1;
        return serverID;
    }

    // Runnable Thread to listen for inbound UDP packets
    Runnable UDPRunnable = new Runnable() {
    public void run (){
        /*synchronized (this) {
            this.runningThread = Thread.currentThread();
        }*/
        //openServerUDPSocket();
        //System.out.println(UDPSocket.getLocalAddress().toString());

        byte[] receiveData = new byte[1024];

        while (true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                dsock.receive(receivePacket);
                String message = receivePacket.getData().toString();
                InetAddress add = receivePacket.getAddress();
                System.out.print("Received: " + message);
                
                if (message.startsWith("Found File/")) {
                    
                
                String[] req = message.split(message);
                
                // Open TCP port to request file from peer
                SocketClient client = new SocketClient(req[2], port, req[1].substring(5));
                new Thread(client).start();
                client.run();
                }

            } catch (IOException e) {
                throw new RuntimeException("Error receive packet.", e);
            }
        }
    }
    };
}
