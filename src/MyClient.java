
import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * MyClient class is an object for an instance of a client
 * node. A client node is capable of querying a server for
 * Time information, and with the correct authorization,
 * can also reset the time that the server holds. More
 * information on the kind of requests and their formats
 * can be found in the documentation folder of the project.
 *
 * Created by kravikum on 2/21/16.
 */
public class MyClient {

    Socket client;
    DatagramSocket clientUdp;
    String server, user, pass, time, params[];
    int port, numberOfRequests, format;
    long roundTripTime;
    static boolean setTime;

    /**
     * Constructor to initialize the client node
     *
     * @param serv Address of the server to query for information
     * @param p Port number of the server
     * @param num Number of requests/queries to make
     * @param setT Boolean to indicate if client wants to reset the time
     * @param time1 Client's time information to send to the server
     * @param usr Username for authentication
     * @param pw Password for authentication
     * @param fmt Format of the time to be displayed
     * @throws IOException
     */
    public MyClient (String serv, int p, int num, boolean setT, String time1, String usr, String pw, int fmt) throws IOException {
        server = serv;
        port = p;
        numberOfRequests = num;
        setTime = setT;
        params = new String[500];
        time = "";
        time = time1;
        user = usr;
        pass = pw;
        format = fmt;
        roundTripTime = 0;
    }

    /**
     * Parse the response obtained from the server and generate
     * a report/message with the appropriate data
     *
     * @param resp The response obtained from the server
     */
    public void parseResponse(String resp) {
        Calendar cal = Calendar.getInstance();
        java.util.Arrays.fill(params, "");
        int x = 1;
        params = resp.split(" ");

        switch (params[0]) {
            case "1":
                System.out.print("Server time set. Current Time: ");
                x++;
                if (format == 1) {
                    params[1] = params[1].trim();
                    cal.setTimeInMillis(Long.parseLong(params[1]) * 1000);
                    DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                    df.setTimeZone(TimeZone.getTimeZone("UTC"));
                    System.out.println(df.format(cal.getTime()));
                    break;
                }
                System.out.println(params[1]);
                break;
            case "2":
                System.out.print("Current Time: ");
                x++;
                if (format == 1) {
                    params[1] = params[1].trim();
                    cal.setTimeInMillis(Long.parseLong(params[1]) * 1000);
                    DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                    df.setTimeZone(TimeZone.getTimeZone("UTC"));
                    System.out.println(df.format(cal.getTime()));
                    break;
                }
                System.out.println(params[1]);
                break;
            case "3":
                System.out.println("Clients cannot change Server time");
                break;
            case "4":
                System.out.println("Improper credentials. Cannot change time");
            case "5":
                System.out.println("Server time not set!");
                break;
            case "6":
                System.out.println("Unrecognized Request");
                break;
            default:
                System.out.println("Parse Error: Response from server could not be parsed");
        }
        System.out.println("-------------------------\n  Hop\t\tRTT");
        System.out.println("-------------------------");
        StringBuilder rttReport = new StringBuilder();
        for(int i = x, j = 0; i < params.length; i++, j++) {
            switch (params[i]) {
                case "-h":
                    long hopTime = roundTripTime - Integer.parseInt(params[++i].trim());
                    hopTime = Math.abs(hopTime);
                    if(j == 0) {
                        rttReport.insert(0, "   Server" + "\t\t" + hopTime + "ms\n");
                    }
                    else {
                        rttReport.insert(0, "   Proxy " + j + "\t\t" + hopTime + "ms\n");
                    }
                    break;
                default:
                    System.out.println("Cannot decipher hop data");
            }
        }
        System.out.println(rttReport.toString());
        System.out.println("-------------------------");
        System.out.println("Total RTT : " + roundTripTime + "ms\n");
    }

    /**
     * Start an instance of the client that uses UDP to communicate
     * with the server
     *
     * @throws IOException
     */
    public void startUDPClient() throws IOException {
        byte[] request, response;
        request = new byte[10000];
        response = new byte[100000];
        clientUdp = new DatagramSocket();
        for (int i = 0; i < numberOfRequests; i++) {
            StringBuilder build = new StringBuilder();
            DatagramPacket spacket = new DatagramPacket(request, request.length);
            DatagramPacket rpacket = new DatagramPacket(response, response.length);
            if(setTime) {
                build.append("-T " + user + " " + pass + " " + time);
            }
            else {
                build.append("-R ");
            }
            request = new byte[build.toString().getBytes().length];
            request = build.toString().getBytes();
            spacket = new DatagramPacket(request, request.length, InetAddress.getByName(server), port);
            long startTime = System.currentTimeMillis();
            clientUdp.send(spacket);
            response = new byte[100000];
            rpacket = new DatagramPacket(response, response.length);
            clientUdp.receive(rpacket);
            long endTime = System.currentTimeMillis();
            roundTripTime = endTime - startTime;
            parseResponse(new String(rpacket.getData(), 0, rpacket.getData().length));
        }
    }

    /**
     * Start an instance of the client that uses TCP to communicate
     * with the server
     *
     * @throws IOException
     */
    public void startTCPClient() throws IOException {
        for (int i = 0; i < numberOfRequests; i++) {
            StringBuilder build = new StringBuilder();
            client = new Socket(server, port);
            DataInputStream clientInput = new DataInputStream(client.getInputStream());
            DataOutputStream clientOutput = new DataOutputStream(client.getOutputStream());
            if(setTime) {
                build.append("-T " + user + " " + pass + " " + time);
                long startTime = System.currentTimeMillis();
                clientOutput.writeUTF(build.toString());
                String response = clientInput.readUTF();
                long endTime = System.currentTimeMillis();
                roundTripTime = endTime - startTime;
                parseResponse(response);
            }
            else {
                clientOutput.writeUTF(build.append("-R ").toString());
                parseResponse(clientInput.readUTF());
            }
            clientInput.close();
            clientOutput.close();
            client.close();
        }
    }

}