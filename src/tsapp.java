/**
 * tsapp serves as a common launcher for client, server and proxy nodes.
 * The kind of node launched depends on the command-line arguments with
 * which this application is launched. Check the documentation for
 * more information on the command-line options.
 *
 * Created by kravikum on 2/21/16.
 */
public class tsapp {

    // Initialize variables to send to client/server/proxy
    int port1, port2, proxyPort1, proxyPort2, mode = -1, format = 0, numberOfRequests = 1;
    char type = 'd';
    boolean setTime = false;
    String serverAddress, time = "", user = "NO-AUTH", pass = "default";

    /**
     * Parse the command-line arguments one by one and launch
     * an instance of a client/server/proxy node accordingly.
     *
     * @param args The list of arguments passed to the launcher via the command-line
     */
    private void parseArguments(String[] args) {

        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            switch (s) {
                case "-c":
                    type = 'c';
                    serverAddress = args[++i];
                    break;
                case "-s":
                    type = 's';
                    break;
                case "-p":
                    type = 'p';
                    serverAddress = args[++i];
                    break;
                case "-u":
                    mode = 0;
                    break;
                case "-t":
                    mode = 1;
                    break;
                case "-z":
                    format = 1;
                    break;
                case "-T":
                    setTime = true;
                    time = "";
                    time = args[++i];
                    break;
                case "--user":
                    user = args[++i];
                    break;
                case "--pass":
                    pass = args[++i];
                    break;
                case "-n":
                    numberOfRequests = Integer.parseInt(args[++i]);
                    break;
                case "--proxy-udp":
                    proxyPort1 = Integer.parseInt(args[++i]);
                    break;
                case "--proxy-tcp":
                    proxyPort2 = Integer.parseInt(args[++i]);
                    break;
                default  :
                    if(type == 'c') {
                        port1 = Integer.parseInt(args[i]);
                    }
                    else if(type == 's' || type == 'p') {
                        port1 = Integer.parseInt(args[i]);
                        port2 = Integer.parseInt(args[++i]);
                    }
            }
        }
    }

    /**
     * The main class of the launcher. It invokes the parseArguments method
     * on the incoming command-line arguments and based on the results,
     * launches an instance of a server/client/proxy node.
     *
     * @param args
     */
    public static void main(String[] args) {
        tsapp la = new tsapp();
        try {
            la.parseArguments(args);

            // Launch Server
            if (la.type == 's') {
                MyServer servUDP = new MyServer(la.port1, la.port2, la.setTime, la.time, la.user, la.pass, 1);
                MyServer servTCP = new MyServer(la.port1, la.port2, la.setTime, la.time, la.user, la.pass, 0);
                new Thread(servUDP).start();
                new Thread(servTCP).start();
            }

            // Launch Client
            else if (la.type == 'c') {
                MyClient client = new MyClient(la.serverAddress, la.port1, la.numberOfRequests, la.setTime, la.time, la.user, la.pass, la.format);
                if (la.mode == 1)
                    client.startTCPClient();
                else
                    client.startUDPClient();
            }

            // Launch Proxy
            else {
                //Proxy
                MyProxy proxyUDPServer = new MyProxy(la.serverAddress, la.port1, la.port2, la.proxyPort1, la.proxyPort2, la.mode, 1);
                MyProxy proxyTCPServer = new MyProxy(la.serverAddress, la.port1, la.port2, la.proxyPort1, la.proxyPort2, la.mode, 0);
                new Thread(proxyUDPServer).start();
                new Thread(proxyTCPServer).start();
            }
        } catch (Exception e) {
            System.out.println("Something went wrong: " + e);
        }
    }

}