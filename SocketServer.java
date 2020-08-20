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
    import java.net.ServerSocket;
    import java.net.Socket;
    import java.io.IOException;
    import java.io.InputStream;
import java.io.InputStreamReader;
    import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.LocalTime;

/**
 * TCP PeerServer to accept incoming connections for requests
 * @author Edward Chiu, Inheung Ji, Brian Luo
 */
public class SocketServer implements Runnable{

    private int serverPort = 8080;
    private ServerSocket serverSocket = null;
    private boolean isStopped = false;

    /**
     *
     */
    protected Thread runningThread = null;

    /**
     *
     * @param port Server Port Number
     */
    public SocketServer(int port) {
        this.serverPort = port;
    }

    /**
     *
     */
    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        
        while(!isStopped()) {
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
                //new Thread(new WorkerRunnable(clientSocket, "Multithreaded Server")).start();
            }
            catch (SocketException e) {
                System.out.println("Socket disconnected.");
                e.printStackTrace();
            }
            catch(IOException e) {
                if (isStopped()) {
                    System.out.println("Server stopped1.");
                    return;
                }
                throw new RuntimeException("Error accepting client connection.", e);
            }
            new Thread(new WorkerRunnable(clientSocket, "Multithreaded Server")).start();
            /*try {
                processClientRequest(clientSocket);
            }
            catch(Exception e) {
                System.out.println("Process Request failed.");
            }*/
            
            /*try (InputStream is = clientSocket.getInputStream();
                BufferedReader input = new BufferedReader(
                    new InputStreamReader(is));
                OutputStream output = clientSocket.getOutputStream();) {

            String in, out;
            in = input.readLine();
            File file = new File("pictures" + File.separator + in.split(" ")[1]);
            System.out.println("From client: " + in);
            int code = checkRequest(in, file);
            if (code == 200) {
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
            System.out.println(Thread.getId());
        } catch (SocketException e) {
            System.out.println("Disconnected socket thread");
            //e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Thread did not process request.");
            e.printStackTrace();
        }*/
        }
        System.out.println("Server stopped2.");
    }

    // Deprecated code because server is multi-threaded
    private void processClientRequest(Socket clientSocket) {
        try {
            //InputStream input = clientSocket.getInputStream();
            //OutputStream output = clientSocket.getOutputStream();
            BufferedReader input = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
            long time = System.currentTimeMillis();

            byte[] responseDoc = ("<html><body>" + "Server: "
                    + time + "</body></html>").getBytes("UTF-8");

            byte[] responseHeader = ("HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/html; charset=UTF-8\r\n"
                    + "Content-Length: " + responseDoc.length
                    + "\r\n\r\n").getBytes();

            //output.write(responseHeader);
            //output.write(responseDoc);
            //output.write("Server says hi".getBytes("UTF-8"));
            String in, out;
            in = input.readLine();
            System.out.println("From client: " + in);
            out = "<html><body>" + "Server: "
                    + time + "</body></html>";
            output.flush();
            output.println(out);
            output.flush();
            out = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/html; charset=UTF-8\r\n"
                    + "Content-Length: " + responseDoc.length
                    + "\r\n\r\n";
            output.println(out);
            output.flush();
            int count = 0;
            
            while((in = input.readLine()) != null) {
               out = Integer.toString(count++);
               output.println(out);
               output.flush();
               System.out.println("From Client: " + in);
               System.out.println("To Client: " + out);
               //output.println(out);
               //output.write(in);
               
               if (out.equals("8")) {
                   isStopped = true;
                   break;
               }
            }
            
            output.close();
            input.close();
            System.out.println("Request processed: " + time);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void sendFile(File file, InputStream is, OutputStream os) throws IOException {
        try (FileInputStream requestedFile = new FileInputStream(file.getPath());
                DataInputStream dis = new DataInputStream(new BufferedInputStream(requestedFile));) {
            byte[] responseDoc = new byte[(int) file.length()];

            dis.readFully(responseDoc, 0, responseDoc.length);

            os.write(responseDoc, 0, responseDoc.length);
            os.flush();

            System.out.println(file.getName() + " sent to client.");
        } catch (Exception e) {
            System.err.println("Error. " + e);
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

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    /**
     *
     */
    public synchronized void stop() {
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port 8080", e);
        }
    }
}
