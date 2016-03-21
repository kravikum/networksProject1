
import java.io.*;
import java.net.*;

/**
 * MyProxy class is an object for an instance of a proxy
 * node. A proxy node acts as a forwarding node, that can
 * respond to requests from a client by querying the server
 * for the information and forwarding the response to the
 * client. More information on the kind of requests and their formats
 * can be found in the documentation folder of the project.
 *
 * Created by kravikum on 2/25/16.
 */
public class MyProxy implements Runnable {

    ServerSocket proxyAsTCPServer;
    DatagramSocket proxyAsUDPServer;
    Socket proxyAsTCPClient;
    DatagramSocket proxyAsUDPClient;

    String serverAddr;

    int clientPortTCP, clientPortUDP, serverPortTCP, serverPortUDP, mode = 0, decide;

    /**
     * Constructor to initialize the proxy node
     *
     * @param serv Address of the server to query
     * @param p1 UDP Port of proxy server
     * @param p2 TCP Port of proxy server
     * @param p3 UDP Port of origin server
     * @param p4 TCP Port of origin server
     * @param m Mode indicator of whether to use TCP/UDP with origin server
     * @param d Decider to indicate whether the proxy would act as a TCP/UDP server
     */
    public MyProxy(String serv, int p1, int p2, int p3, int p4, int m, int d) {
        serverAddr = serv;
        clientPortUDP = p1;
        clientPortTCP = p2;
        serverPortUDP = p3;
        serverPortTCP = p4;
        mode = m;
        decide = d;
    }

    /**
     * Starts a UDP or TCP proxy server based on the decider
     */
    public void run() {
        try {
            if(decide == 1)
                startUDPServer();
            else
                startTCPServer();
        } catch (Exception e) {
            System.out.println("Something went wrong: " + e);
        }
    }

    /**
     * Start a UDP Proxy server
     *
     * @throws IOException
     */
    private void startUDPServer() throws IOException {

        // UDP initializations
        DatagramPacket packet;
        byte[] request, response;
        request = new byte[10000];
        response = new byte[10000];
        proxyAsUDPClient = new DatagramSocket();
        proxyAsUDPServer = new DatagramSocket(clientPortUDP);
        DatagramPacket reqPacket = new DatagramPacket(request, request.length);
        DatagramPacket resPacket = new DatagramPacket(response, response.length);

        // Client --UDP--> Proxy --UDP--> Server
        if(mode == -1 || mode == 0) {
            while (true) {
                reqPacket = new DatagramPacket(request, request.length);
                proxyAsUDPServer.receive(reqPacket);
                request = new byte[10000];
                request = reqPacket.getData();
                packet = new DatagramPacket(request, request.length, InetAddress.getByName(serverAddr), serverPortUDP);
                long startTime = System.currentTimeMillis();
                proxyAsUDPClient.send(packet);
                response = new byte[request.length + 10000];
                resPacket = new DatagramPacket(response, response.length);
                proxyAsUDPClient.receive(resPacket);
                long endTime = System.currentTimeMillis();
                long roundTripTime = endTime - startTime;
                String temp = new String(resPacket.getData(), 0, resPacket.getData().length);
                temp = temp.concat(" -h " + roundTripTime);
                response = new byte[temp.getBytes().length];
                response = temp.getBytes();
                resPacket = new DatagramPacket(response, response.length, reqPacket.getAddress(), reqPacket.getPort());
                proxyAsUDPServer.send(resPacket);
            }
        }

        // Client --UDP--> Proxy --TCP--> Server
        else {
            while (true) {
                proxyAsUDPServer.receive(reqPacket);

                // TCP initializations
                proxyAsTCPClient = new Socket(serverAddr, serverPortTCP);
                DataInputStream proxyClientInput = new DataInputStream(proxyAsTCPClient.getInputStream());
                DataOutputStream proxyClientOutput = new DataOutputStream(proxyAsTCPClient.getOutputStream());

                String req = new String(reqPacket.getData(), 0, reqPacket.getData().length);
                long startTime = System.currentTimeMillis();
                proxyClientOutput.writeUTF(req);
                String res = proxyClientInput.readUTF();
                long endTime = System.currentTimeMillis();
                long roundTripTime = endTime - startTime;
                res = res.concat(" -h " + roundTripTime);
                response = new byte[res.getBytes().length];
                response = res.getBytes();
                resPacket = new DatagramPacket(response, response.length, reqPacket.getAddress(), reqPacket.getPort());
                proxyAsUDPServer.send(resPacket);

                proxyClientInput.close();
                proxyClientOutput.close();
                proxyAsTCPClient.close();
            }
        }
    }

    /**
     * Start a TCP Proxy server
     *
     * @throws IOException
     */
    private void startTCPServer() throws IOException {
        // TCP initializations
        proxyAsTCPServer = new ServerSocket(clientPortTCP);

        // UDP initializations
        DatagramPacket packet;
        byte[] request, response;
        request = new byte[10000];
        response = new byte[10000];
        proxyAsUDPClient = new DatagramSocket();

        // Client --TCP--> Proxy --TCP--> Server
        if(mode == -1 || mode == 1) {
            while (true) {
                Socket server = proxyAsTCPServer.accept();

                proxyAsTCPClient = new Socket(serverAddr, serverPortTCP);
                DataInputStream proxyClientInput = new DataInputStream(proxyAsTCPClient.getInputStream());
                DataOutputStream proxyClientOutput = new DataOutputStream(proxyAsTCPClient.getOutputStream());

                DataInputStream servInput = new DataInputStream(server.getInputStream());
                DataOutputStream servOutput = new DataOutputStream(server.getOutputStream());
                long startTime = System.currentTimeMillis();
                proxyClientOutput.writeUTF(servInput.readUTF());
                String send = proxyClientInput.readUTF();
                long endTime = System.currentTimeMillis();
                long roundTripTime = endTime - startTime;
                send = send.concat(" -h " + roundTripTime);
                servOutput.writeUTF(send);

                servInput.close();
                servOutput.close();
                proxyClientInput.close();
                proxyClientOutput.close();
                proxyAsTCPClient.close();
            }
        }

        // Client --TCP--> Proxy --UDP--> Server
        else {
            while (true) {
                response = new byte[10000];
                request = new byte[10000];
                DatagramPacket reqPacket = new DatagramPacket(request, request.length);
                DatagramPacket resPacket = new DatagramPacket(response, response.length);
                Socket server = proxyAsTCPServer.accept();
                DataInputStream servInput = new DataInputStream(server.getInputStream());
                DataOutputStream servOutput = new DataOutputStream(server.getOutputStream());
                String req = servInput.readUTF();
                request = req.getBytes();
                reqPacket = new DatagramPacket(request, request.length, InetAddress.getByName(serverAddr),
                        serverPortUDP);
                long startTime = System.currentTimeMillis();
                proxyAsUDPClient.send(reqPacket);
                response = new byte[request.length + 10000];
                resPacket = new DatagramPacket(response, response.length);
                proxyAsUDPClient.receive(resPacket);
                long endTime = System.currentTimeMillis();
                long roundTripTime = endTime - startTime;
                String res = new String(resPacket.getData(), 0, resPacket.getData().length);
                res = res.concat(" -h " + roundTripTime);
                servOutput.writeUTF(res);
                servInput.close();
                servOutput.close();
            }
        }
    }

}
