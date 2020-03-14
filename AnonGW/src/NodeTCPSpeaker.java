import java.io.*;
import java.net.Socket;
import java.util.Queue;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

public class NodeTCPSpeaker implements Runnable{
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private SortedSet<Request> requests;
    private String target_address;
    private volatile boolean running = true;

    public NodeTCPSpeaker(Socket s, SortedSet<Request> r, String target) {
        this.socket = s;
        this.requests = r;
        this.target_address = target;
    }


    public void run() {
        try {
            while (true) {
                TimeUnit.SECONDS.sleep(2);
                if (requests.size() > 0) {
                    Request r = requests.first();
                    if (r.getStatus().equals("na")) {
                        r.setStatus("ad");
                        System.out.println("> Speaker: Found request!");


                        System.out.println("> Speaker: Sent request to server");
                        // Envia o pedido ao servidor de destino
                        PrintWriter pw = new PrintWriter(socket.getOutputStream());
                        pw.println(r.getMessage());
                        pw.println();
                        pw.flush();


                        System.out.println("> Speaker: Getting response from server");
                        // Recebe a resposta do servidor de destino
                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String t;
                        while ((t = br.readLine()) != null)
                            r.concatenateResponse(t);
                        r.setStatus("sd");

                        System.out.println("> Speaker: Request has been served at destination!");
                        br.close();

                        //r.printRequest();
                    }
                }

            }
            //socket.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void stopConnection() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
