package Model;

import java.io.*;
import java.net.Socket;
import java.util.Queue;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

public class NodeTCPListener implements Runnable {

    private String target_address;
    private String source_address;
    private Socket socket;
    private SortedSet<Request> requests;
    private Boolean running;

    final String secretKey = "HelpMeObiWanKenobi!";

    public NodeTCPListener(Socket s, SortedSet<Request> r, String address) {
        this.socket = s;
        this.requests = r;
        this.target_address = address;
        this.running = true;
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

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(socket.getOutputStream()), "UTF-8"));

            final String data = in.readLine();
            System.out.println("> Listener: Established new connection with outside");
                if (!socket.getRemoteSocketAddress().toString().equals(target_address)) {
                    final Request r = new Request(socket.getRemoteSocketAddress().toString().substring(1),secretKey);
                    r.setMessage(data,secretKey);

                    //r.printRequest();

                    if (!repeatedRequest(socket.getRemoteSocketAddress().toString().substring(1), data)) {
                        this.requests.add(r);
                        out.write("");
                        System.out.println("> Listener: Added new request");
                        System.out.println("> Listener: Queue size is " + requests.size());


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
<<<<<<< HEAD:AnonGW/src/NodeTCPListener.java
                                            if (r.getStatus(secretKey).equals("sd")) {
                                                System.out.println("> Listener: Request has been served at destination!");
                                                r.setStatus("to",secretKey);
=======
                                            if (r.getStatus().equals("sd")) {
                                                System.out.println("> Listener: Model.Request has been served at destination!");
                                                r.setStatus("to");
>>>>>>> 4cd0c915e1d28993aa03256af811f5630484d8f6:src/Model/NodeTCPListener.java

                                                //Envia a resposta
                                                Object[] rarray = r.getResponse(secretKey);
                                                for (Object s : rarray)
                                                    out.write(s.toString());

                                                out.flush();
<<<<<<< HEAD:AnonGW/src/NodeTCPListener.java
                                                r.setStatus("so",secretKey);
                                                System.out.println("> Listener: Request has been served at origin!");
=======
                                                r.setStatus("so");
                                                System.out.println("> Listener: Model.Request has been served at origin!");
>>>>>>> 4cd0c915e1d28993aa03256af811f5630484d8f6:src/Model/NodeTCPListener.java

                                                requests.remove(r);
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
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
