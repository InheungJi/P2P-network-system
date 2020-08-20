/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p1;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Scanner;
/**
 *
 * @author Edward Chiu, Inheung Ji, Brian Luo
 */
public class SocketClient implements Runnable {
    private String outboundIP;
    private int clientPort = 8080;
    private int serverPort = 0;
    private Socket clientSocket = null;
    //private boolean isStopped = false;
    protected Thread runningThread = null;
    String filename; 
    
    public SocketClient(int port) {
        this.serverPort = port;
    }
    public SocketClient(String outboundIP, int port, String filename) {
        this.outboundIP = outboundIP;
        this.serverPort = port;
        this.filename = filename;
    }
    
    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        
        openClientSocket();
        
        //while (!isStopped) {
            InetSocketAddress add = new InetSocketAddress(InetAddress.getLoopbackAddress(), clientPort);
            InetSocketAddress socketAddress = new InetSocketAddress(clientPort);
            
            try {
                makeRequest(this.clientSocket.getLocalSocketAddress());
                clientSocket.close();
                //this.isStopped = true;
            }
            catch (SocketException e) {
               System.out.println("Disconnected client socket.");
               return;
            }
            catch (Exception e) {
                throw new RuntimeException("Request to server failed.", e);
            }
            finally {
                //this.isStopped = true;
                //this.runningThread.interrupt();
                //clientSocket.close();
                System.out.println("Client closed connection to server.");
                //return;
            }
        //}
        //this.isStopped = true;
        //System.out.println("Client closed connection to server.");
    }
    
    private void makeRequest(SocketAddress address) {
        try (InputStream is = this.clientSocket.getInputStream();
                OutputStream output = clientSocket.getOutputStream();
                BufferedReader input = new BufferedReader(new InputStreamReader(is));){
            
            long time = System.currentTimeMillis();
            byte[] requestDoc = ("<html><body>" + "Hello Server: "
                    + time + "</body></html>").getBytes("UTF-8");

            byte[] requestHeader = ("HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/html; charset=UTF-8\r\n"
                    + "Content-Length: " + requestDoc.length
                    + "\r\n\r\n").getBytes("UTF-8");
            
            String method = "GET ";
            //String filepath = "fanBlink.jpg ";
            final String httpV = "HTTP/1.1\r\n";

            output.write((method + this.filename + httpV + "\r\n").getBytes("UTF-8"));
            output.flush();
            String in = "";
            
            /*URLConnection local = new URL("http://localhost:4444").openConnection();
            //local.setRequestProperty("Accept-Charset", java.nio.charset.StandardCharsets.UTF_8.name());
            HttpURLConnection cc = ((HttpURLConnection) local);
            cc.setRequestMethod("GET");
            cc.setRequestProperty("Content-Length", "");
            System.out.println("cc: " + cc.getHeaderField("Content-Length"));*/
            long filesize = 0;
            boolean done = false;
            while (!done & (in = input.readLine()) != null) {
                //in = input.readLine();
                System.out.println("From Server: " + in);
                if (in.contains("Content-Length:")) {
                    filesize = Long.parseLong(in.substring(16));
                    System.out.println("Filesize: " + filesize);
                    //input.readLine();
                    //receiveFile("fanBlink.jpg", filesize, is);
                    //receiveFile("packageCompile.txt");
                    done = true;
                    break;
                }
            }
            //receiveFile("packageCompile.txt", filesize, is, output);
            receiveFile(filesize, is, output);
            
            //output.close();
            //input.close();
            //String s = new String(input, StandardCharsets.UTF_8);
            //System.out.println(input.toString());
            String now = LocalTime.now().getHour() + ":" + LocalTime.now().getMinute()
                    + ":" + LocalTime.now().getSecond();
            System.out.println("Request received: " + now);
            System.out.println(runningThread.getId());
            //this.isStopped = true;
        }
        catch (UnknownHostException e) {
            System.err.println("Don't know about host ");
            System.exit(1);
        }
        catch (IOException e) {
            e.printStackTrace();
            //System.exit(1);
        }
    }
    
    /*private synchronized boolean isStopped() {
        return this.isStopped;
    }
    public synchronized void stop() {
        this.isStopped = true;
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }*/
    
    private void openClientSocket() {
        try {
            // Loopback Address is for testing client
            // Local IP address loopback = 127.0.0.1
            //InetAddress address = InetAddress.getLoopbackAddress();
            InetAddress address = InetAddress.getByName(outboundIP);
            this.clientSocket = new Socket(address, serverPort);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot connect to port on server", e);
        }
    }
    
    private void receiveFile(long size, InputStream is, OutputStream os) throws IOException {
        try {
            int bytesRead;
            /*os = new FileOutputStream("pictures" + File.separator + "received" + filename);
            
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = is.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
                os.flush();
                os.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            os.flush();
            //output.flush();*/
            
            File fileR = new File("pictures" + File.separator + "received" + this.filename);
            Path target = Paths.get(fileR.getPath());
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            
            if (fileR.exists()) {
            System.out.println(this.filename + " received from server." + fileR.length());
            /*if (fileR.length() == size) {
                this.isStopped = true;
            }
            else {
                this.isStopped = false;
            }*/
            }
        } catch (IOException e) {
            throw new RuntimeException("File not received.");
        }
    }
}
