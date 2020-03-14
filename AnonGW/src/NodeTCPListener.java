import java.io.*;
import java.net.Socket;
import java.util.Queue;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

public class NodeTCPListener implements Runnable{
    private String target_address;
    private String source_address;
    private Socket socket;
    private SortedSet<Request> requests;
    private Boolean running;

    public NodeTCPListener(Socket s, SortedSet<Request> r, String address) {
        this.socket = s;
        this.requests = r;
        this.target_address = address;
        this.running = true;
    }

    private boolean repeatedRequest(String sourceAddress, String request){
        boolean r = false;

        if (this.requests.size() > 0){
            for (Request req : this.requests)
                if (req.getOrigin_address().equals(sourceAddress))
                    r = true;
        }

        return r;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(socket.getOutputStream()), "UTF-8"));

            final String data = in.readLine();

            while (data != null && running) {
                //System.out.println(data);

                if (!socket.getRemoteSocketAddress().toString().equals(target_address)) {
                    final Request r = new Request(socket.getRemoteSocketAddress().toString().substring(1));
                    r.setMessage(data);

                    //r.printRequest();

                    if (!repeatedRequest(socket.getRemoteSocketAddress().toString().substring(1), data)) {
                        this.requests.add(r);
                        System.out.println("> Listener: Added new request");
                        System.out.println("> Listener: Queue size is " + requests.size());

                        Thread answer = new Thread(){
                            public void run() {
                                while (true) {
                                    try {
                                        TimeUnit.SECONDS.sleep(5);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    while (!r.getStatus().equals("so")) {
                                        try {
                                            if (r.getStatus().equals("sd")) {
                                                System.out.println("> Listener: Request has been served at destination!");
                                                r.setStatus("to");

                                                //Envia a resposta
                                                Object[] rarray = r.getResponse();
                                                for (Object s : rarray)
                                                    out.write(s.toString());

                                            /* This horseshit works
                                            out.write("HTTP/1.1 200 Ok\r\n");
                                            out.write("Content-Type: text/html\r\n");
                                            out.write("Content-Length: 12");
                                            out.write("\r\n\r\n");
                                            out.write("it's a trap!");
                                            */

                                                out.flush();
                                                r.setStatus("so");
                                                System.out.println("> Listener: Request has been served at origin!");
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    //requests.remove(r);
                                    //System.out.println("> Listener: Request has been removed from Queue!");
                                    running = false;
                                }
                            }
                        };

                        answer.start();
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
