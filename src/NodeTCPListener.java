import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.SortedSet;

public class NodeTCPListener implements Runnable {

    private String target_address;
    private String source_address;
    private Socket socket;
    private DatagramSocket UDPsocket;
    private SortedSet<Request> requests;
    private Boolean running;
    private Set<String> peers;
    private int protected_port;

    final String secretKey = "HelpMeObiWanKenobi!";

    public NodeTCPListener(Socket s, SortedSet<Request> r, String address, DatagramSocket usocket, Set<String> p, int port) {
        this.socket = s;
        this.requests = r;
        this.target_address = address;
        this.running = true;
        this.UDPsocket = usocket;
        this.peers = p;
        this.protected_port = port;
    }

    private int random(int lower, int upper){
        return  (int) (Math.random() * (upper - lower)) + lower;
    }

    private String getPeer(){
        int i = random(0,peers.size());
        String[] ps = peers.toArray(new String[peers.size()]);
        return ps[i];
    }

    private boolean repeatedRequest(String sourceAddress, String request) {
        boolean r = false;

        if (this.requests.size() > 0){
            for (Request req : this.requests)
                if (req.getOrigin_address(secretKey).equals(sourceAddress))
                    r = true;
        }

        return r;
    }

    public void startRequestHandler(DatagramSocket s,Request r, int port) throws IOException {

        Thread handler = new Thread(){
            public void run(){
                while (true) {
                    Socket socket = null;
                    try {
                        RequestHandler rh = new RequestHandler(s,r,getPeer(),port);
                        new Thread(rh).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
                    final Request r = new Request(socket.getRemoteSocketAddress().toString().substring(1),secretKey);
                    r.setMessage(data,secretKey);

                    //r.printRequest();

                    System.out.println("> TCPListener: Created the new Request");
                    //if (!repeatedRequest(socket.getRemoteSocketAddress().toString().substring(1), data)) {
                        //this.requests.add(r);
                        out.write("");

                        startRequestHandler(this.UDPsocket,r,this.protected_port);
                        System.out.println("> TCPListener: Sent the new Request");
                        //System.out.println("> Listener: Queue size is " + requests.size());


                                while (running) {
                                    /*
                                    try {
                                        TimeUnit.SECONDS.sleep(5);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }*/
                                    while (!r.getStatus(secretKey).equals("so")) {
                                        //System.out.println(r.getStatus());
                                        try {
                                            if (r.getStatus(secretKey).equals("sd")) {
                                                System.out.println("> Listener: Request has been served at destination!");
                                                r.setStatus("to",secretKey);

                                                //Envia a resposta
                                                Object[] rarray = r.getResponse(secretKey);
                                                for (Object s : rarray)
                                                    out.write(s.toString());

                                                out.flush();

                                                r.setStatus("so",secretKey);
                                                System.out.println("> Listener: Request has been served at origin!");

                                                requests.remove(r);
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    System.out.println("> Listener: Model.Request has been removed from Queue!");
                                    running = false;
                                }


                    //}



            }



            socket.close();
            out.flush();
            System.out.println("> Listener: I'm dead inside :(");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
