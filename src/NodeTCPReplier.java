import java.io.*;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class NodeTCPReplier implements Runnable {

    private Socket socket;
    private SortedSet<Request> replies;
    private Boolean running;
    private String clientaddress;
    private BufferedWriter out;

    final String secretKey = "HelpMeObiWanKenobi!";

    public NodeTCPReplier(Socket s, SortedSet<Request> r,BufferedWriter out, String client) {
        this.socket = s;
        this.replies = r;
        this.clientaddress = client;
        this.out = out;
    }

    public void run() {
        try {
            while (running) {
                try {
                    TimeUnit.MILLISECONDS.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (replies.size() > 0) {
                    Request requesttoserve = replies.first();
                    for (Iterator<Request> it = replies.iterator(); it.hasNext(); ) {
                        Request f = it.next();
                        if (f.getOrigin_address(secretKey).equals(clientaddress))
                            requesttoserve = f;
                    }

                    if(!requesttoserve.getStatus(secretKey).equals("so") && requesttoserve.getOrigin_address(secretKey).equals(clientaddress)) {
                        try {
                            if (requesttoserve.getStatus(secretKey).equals("sd")) {
                                System.out.println("> TCPReplier: Request has been served at destination!");
                                requesttoserve.setStatus("to", secretKey);

                                //Envia a resposta
                                Object[] rarray = requesttoserve.getResponse(secretKey);
                                for (Object s : rarray)
                                    out.write(s.toString());

                                out.flush();

                                requesttoserve.setStatus("so",secretKey);
                                System.out.println("> TCPReplier: Request has been served at origin!");

                                replies.remove(requesttoserve);
                                System.out.println("> TCPReplier: Request has been removed from Queue!");

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    running = false;
                }
            }

            socket.close();
            out.flush();
            System.out.println("> TCPReplier: Job Ended");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
