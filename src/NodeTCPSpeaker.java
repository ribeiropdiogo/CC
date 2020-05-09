import java.io.*;
import java.net.Socket;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

public class NodeTCPSpeaker implements Runnable {

    private Socket external_socket_out;
    private PrintWriter out;
    private BufferedReader in;
    private int outside_port;
    private SortedSet<Request> requests;
    private String target_address;
    private volatile boolean running = true;
    private int contador = 0;

    final String secretKey = "HelpMeObiWanKenobi!";


    public NodeTCPSpeaker(int s, SortedSet<Request> r, String target) {
        this.outside_port = s;
        this.requests = r;
        this.target_address = target;
    }


    public void run() {
        try {
            while (running) {
                try {
                    TimeUnit.MILLISECONDS.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (requests.size() > 0) {
                    external_socket_out = new Socket(target_address, outside_port);
                    Request r = requests.first();
                    //r.printRequest();
                    if (r.getStatus(secretKey).equals("na")) {
                        r.setStatus("ad",secretKey);
                        System.out.println("> Speaker: Found request!");


                        System.out.println("> Speaker: Sent request to server");
                        // Envia o pedido ao servidor de destino
                        PrintWriter pw = new PrintWriter(external_socket_out.getOutputStream());
                        pw.println(r.getMessage(secretKey));
                        pw.println();
                        pw.flush();


                        System.out.println("> Speaker: Getting response from server");
                        // Recebe a resposta do servidor de destino
                        BufferedReader br = new BufferedReader(new InputStreamReader(external_socket_out.getInputStream()));
                        String t;
                        while ((t = br.readLine()) != null)
                            r.concatenateResponse(t,secretKey);
                        r.setStatus("sd",secretKey);

                        System.out.println("> Speaker: Model.Request has been served at destination!");
                        br.close();

                        //r.printRequest();
                        external_socket_out.close();
                    }
                }

            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
