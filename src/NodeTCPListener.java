import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class NodeTCPListener implements Runnable {

    private String target_address, node_address;
    private String source_address;
    private Socket socket;
    private DatagramSocket UDPsocket;
    private SortedSet<Request> requests;
    private Boolean running;
    private Set<String> peers;
    private List<String> waitinglist;
    private int protected_port;

    final String secretKey = "HelpMeObiWanKenobi!";

    public NodeTCPListener(Socket s, SortedSet<Request> r, String address, String naddress, DatagramSocket usocket, Set<String> p, int port) {
        this.socket = s;
        this.requests = r;
        this.target_address = address;
        this.node_address = naddress;
        this.running = true;
        this.UDPsocket = usocket;
        this.peers = p;
        this.protected_port = port;
        this.waitinglist =  new ArrayList<>();
    }

    private int random(int lower, int upper){
        return  (int) (Math.random() * (upper - lower)) + lower;
    }

    private String getPeer(){
        int i = random(0,peers.size());
        String[] ps = peers.toArray(new String[peers.size()]);
        return ps[i];
    }

    private boolean repeatedRequest(String sourceAddress) {
        boolean r = false;

        if (waitinglist.contains(sourceAddress))
            r = true;

        return r;
    }

    public void startRequestHandler(DatagramSocket s,Request r, int port) throws IOException {

        Thread handler = new Thread(){
            public void run(){
                    Socket socket = null;
                    try {
                        RequestHandler rh = new RequestHandler(s,r,getPeer(),port);
                        new Thread(rh).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }
        };
        handler.start();
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(socket.getOutputStream()), "UTF-8"));

            final String data = in.readLine();
            System.out.println("> TCPListener: Established new connection with outside");
                if (!socket.getRemoteSocketAddress().toString().equals(target_address)) {
                    final Request r = new Request(this.node_address,socket.getRemoteSocketAddress().toString().substring(1),secretKey);
                    r.setMessage(data,secretKey);
                    r.setContactNodeAddress(this.node_address,secretKey);
                    r.getStatus(secretKey);
                    //r.printRequest();

                    System.out.println("> TCPListener: Created the new Request");
                    if (!repeatedRequest(socket.getRemoteSocketAddress().toString().substring(1))) {
                        this.waitinglist.add(socket.getRemoteSocketAddress().toString().substring(1));
                        //this.requests.add(r);
                        out.write("");
                        startRequestHandler(this.UDPsocket,r,this.protected_port);
                        System.out.println("> TCPListener: Sent the new Request");
                        //System.out.println("> Listener: Queue size is " + requests.size());


                                while (running) {
                                    Request fisrtrequest = requests.first();

                                    while (!fisrtrequest.getStatus(secretKey).equals("so")) {
                                        //System.out.println(r.getStatus());
                                        try {
                                            if (fisrtrequest.getStatus(secretKey).equals("sd")) {
                                                System.out.println("> TCPListener: Request has been served at destination!");
                                                fisrtrequest.setStatus("to",secretKey);

                                                //Envia a resposta
                                                Object[] rarray = fisrtrequest.getResponse(secretKey);
                                                for (Object s : rarray)
                                                    out.write(s.toString());

                                                out.flush();

                                                fisrtrequest.setStatus("so",secretKey);
                                                System.out.println("> TCPListener: Request has been served at origin!");

                                                requests.remove(fisrtrequest);
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }


                                    }*/
                                    System.out.println("> Listener: Model.Request has been removed from Queue!");
                                    running = false;
                                }


                    }



            }



            socket.close();
            out.flush();
            System.out.println("> Listener: I'm dead inside :(");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
