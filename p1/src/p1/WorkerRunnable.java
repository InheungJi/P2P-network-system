/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p1;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Hashtable;

/**
 * Thread Worker for TCP PeerServer that will process the requests
 * @author Edward Chiu, Inheung Ji, Brian Luo
 */
public class WorkerRunnable implements Runnable {

    /**
     *
     */
    protected ServerSocket serverSocket = null;

    /**
     *
     */
    protected int serverPort = 8080;

    /**
     *
     */
    protected Socket clientSocket = null;

    /**
     *
     */
    protected String serverText = null;
    private boolean isStopped = false;

    /**
     *
     */
    protected Thread runningThread = null;

    /**
     *
     * @param clientSocket
     * @param serverText
     */
    public WorkerRunnable(Socket clientSocket, String serverText) {
        this.clientSocket = clientSocket;
        this.serverText = serverText;
    }

    /**
     *
     */
    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        try (InputStream is = this.clientSocket.getInputStream();
                BufferedReader input = new BufferedReader(
                    new InputStreamReader(is));
                OutputStream output = clientSocket.getOutputStream();) {
            //BufferedReader input = new BufferedReader(
            //        new InputStreamReader(clientSocket.getInputStream()));
            //PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
            //OutputStream output = clientSocket.getOutputStream();
            long time = System.currentTimeMillis();
            //output.flush();
            //output.write("HTTP/1.1 200 OK\r\n".getBytes("UTF-8"));
            //output.flush();
            //System.out.println("To Client: HTTP/1.1 200 OK");

            String in, out;
            in = input.readLine();
            System.out.println("From client: " + in);
            //output.write("HTTP/1.1 200 OK\r\n".getBytes("UTF-8"));
            //out = "<html><body>" + "Server: "
            //        + time + "</body></html>";
            //output.flush();
            //output.println(out);
            /*output.flush();
            out = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/html; charset=UTF-8\r\n"
                    + "Content-Length: " + out.length()
                    + "\r\n\r\n";
            output.println(out);
            output.flush();*/
            File file = new File("pictures" + File.separator + in.split(" ")[1]);
            int code = checkRequest(in, file);
            System.out.println("pictures" + File.separator + in.split(" ")[1] + " " + code);
            if (code == 200) {
                //File file = new File("pictures" + File.separator + in.split(" ")[1]);
                if (file.exists()) {
                    long filesize = file.length();
                    String filetype = "jpg";
                    byte[] responseHeader = ("HTTP/1.1 200 OK\r\n"
                    + "Content-Type: image/jpeg; charset=UTF-8\r\n"
                    + "Content-Length: " + file.length()
                    + "\r\n\r\n").getBytes("UTF-8");
                    output.write(responseHeader);
                    System.out.println("To Client: HTTP Response " + file.length());
                    sendFile(file, is, output);
                }
            }
            output.close();
            input.close();
            String now = LocalTime.now().getHour() + ":" + LocalTime.now().getMinute()
                    + ":" + LocalTime.now().getSecond() + "";
            System.out.println("Request Processed: " + now);
            InetSocketAddress socketAddress = new InetSocketAddress(clientSocket.getPort());
            System.out.println(socketAddress);
        } catch (SocketException e) {
            System.out.println("Disconnected socket thread");
            //e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Thread did not process request.");
            e.printStackTrace();
        }
        finally {
            System.out.println("End of worker thread");
            //this.runningThread.interrupt();
            //return;
        }
    }
    
    private int checkRequest(String request, File file) {
        int code = 200;
        String [] req = request.split(" ");
        
        if (!request.toUpperCase().startsWith("GET ")) {
            code = 400;
        }
        else if (! (file.exists())) {
            code = 404;
        }
        else if (! req[2].equals("HTTP/1.1")) {
            code = 505;
        }
        return code;
    }
    
    private Hashtable getPhrase() {
        Hashtable statusCode = new Hashtable(4);
        statusCode.put(200, "OK");
        statusCode.put(400, "Bad Request");
        statusCode.put(404, "Not Found");
        statusCode.put(505, "HTTP Version Not Supported");
        
        return statusCode;
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    /*public synchronized void stop() {
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing Worker", e);
        }
    }*/

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port 8080", e);
        }
    }

    private void sendFile(File file, InputStream is, OutputStream os) throws IOException {
        try (FileInputStream requestedFile = new FileInputStream(file);
                DataInputStream dis = new DataInputStream(new BufferedInputStream(requestedFile));) {
            byte[] responseDoc = new byte[(int) file.length()];

            /*dis.readFully(responseDoc, 0, responseDoc.length);

            os.write(responseDoc, 0, responseDoc.length);
            os.flush();*/
            
            InetAddress a = clientSocket.getLocalAddress();
            System.out.println(a);
            Path source = Paths.get(file.getPath());
            Path newDir = Paths.get(a.getHostAddress());
            Files.copy(source, os);
            //Files.copy(source, newDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);

            System.out.println(file.getName() + " sent to client.");
        } catch (Exception e) {
            System.err.println("Error. " + e);
        }
    }
}
