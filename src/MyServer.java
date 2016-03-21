
import java.io.*;
import java.net.*;
import java.util.Calendar;

/**
 * MyServer class is an object for an instance of a server
 * node. A server node responds to Time information requests
 * from clients, and allows clients with valid authorization
 * to modify the time that the server holds. More
 * information on the kind of requests and their formats
 * can be found in the documentation folder of the project.
 *
 * Created by kravikum on 2/21/16.
 */
public class MyServer implements Runnable {

    static ServerSocket servSock;
    static DatagramSocket serverUDP;
    int port1, port2, mode = 0;
    static String time, user, pass, params[];
    static boolean setInitialTime = false;
    String myAddress;

    /**
     * Constructor to initalize the server node
     *
     * @param p1 UDP port of the server
     * @param p2 TCP port of the server
     * @param t Boolean for indication of time change
     * @param time1 Initial time of the server
     * @param usr Username authorized to make changes
     * @param pw Password authorized to make changes
     * @param m Mode of operation of the server (TCP/UDP)
     * @throws IOException
     */
    public MyServer (int p1, int p2, boolean t, String time1, String usr, String pw, int m) throws IOException {
        port1 = p1;
        port2 = p2;
        setInitialTime = t;
        params = new String[500];
        time = "";
        time = time1;
        user = usr;
        pass = pw;
        mode = m;
        myAddress = "";
    }

    /**
     * Parse the request from the client and send the
     * appropriate response
     *
     * @param req The incoming request from the client
     * @return The response to the client request
     */
    private String parseRequest(String req) {
        java.util.Arrays.fill(params, "");
        params = req.split(" ");
        String tag = new String("-h 0");
        StringBuilder response = new StringBuilder();
        switch (params[0]) {
            case "-T":
                if(user.equals("NO-AUTH"))
                    return response.append("3 " + tag).toString();
                else if(user.equals(params[1]) && pass.equals(params[2])) {
                    time = "";
                    time = params[3];
                    return response.append("1 " + time + " " + tag).toString();
                }
                else
                    return response.append("4 " + tag).toString();
            case "-R":
                if(time.equals("")) {
                    return response.append("5 " + tag).toString();
                }
                return response.append("2 " + time + " " + tag).toString();
            default:
                return response.append("6 " + tag).toString();
        }
    }

    /**
     * Start a UDP/TCP server based on the mode of operation
     */
    public void run() {
        try {
            if(mode == 1)
                startUDPServer();
            else
                startTCPServer();
        } catch (Exception e) {
            System.out.println("Something went wrong: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Start a UDP server node
     *
     * @throws IOException
     */
    private void startUDPServer() throws IOException {
        byte[] request = new byte[100000];
        serverUDP = new DatagramSocket(port1);
        DatagramPacket reqPacket = new DatagramPacket(request, request.length);
        while (true) {
            serverUDP.receive(reqPacket);
            String response = parseRequest(new String(reqPacket.getData(), 0, reqPacket.getData().length));
            DatagramPacket responsePacket = new DatagramPacket(response.getBytes(), response.length(),
                    reqPacket.getAddress(), reqPacket.getPort());
            serverUDP.send(responsePacket);
        }
    }

    /**
     * Start a TCP server node
     *
     * @throws IOException
     */
    private void startTCPServer() throws IOException {
        servSock = new ServerSocket(port2);
        while (true) {

            Socket server = servSock.accept();

            DataInputStream servInput = new DataInputStream(server.getInputStream());
            DataOutputStream servOutput = new DataOutputStream(server.getOutputStream());

            String request = servInput.readUTF();
            String response = parseRequest(request);
            servOutput.writeUTF(response);
            servInput.close();
            servOutput.close();
        }
    }

}