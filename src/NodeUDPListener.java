import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class NodeUDPListener implements Runnable{

    private PrintWriter out;
    private BufferedReader in;
    private SortedSet<Request> requests, replies;
    private Set<String> peers;
    private DatagramSocket socket;
    private volatile boolean running = true;
    private byte[] buffer = new byte[20*1024];
    private byte[] requestBuffer = new byte[20*1024];
    private InetAddress address;
    private Map<String,SortedSet<PDU>> pduPackets; //ainda não está implementado, mas em principio vamos armazenar aqui os pacotes que chegam ao nodo enquando não chegaram todos os seus parceiros

    final String secretKey = "HelpMeObiWanKenobi!";

    public NodeUDPListener(DatagramSocket socket, SortedSet<Request> r, SortedSet<Request> rep) {
        try {
            this.socket = socket;
            this.requests = r;
            this.replies = rep;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public void run() {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (running) {
            try {
                // receber pacotes udp
                socket.receive(packet);
                System.out.println("> UDPListener: Receiving packet");
                requestBuffer = packet.getData();


                System.out.println("> UDPListener: packet received");
                // colocar esses pacotes udp na fila de espera
                Request r = (Request)deserialize(requestBuffer);
                Arrays.fill(requestBuffer, (byte)0);
                System.out.println("> UDPListener: Converting packet to Request");
                if (r.getStatus(secretKey).equals("na")) {
                    requests.add(r);
                    System.out.println("> UDPListener: Request added to queue");
                } else if (r.getStatus(secretKey).equals("sd")){
                    replies.add(r);
                    System.out.println("> UDPListener: Reply added to queue");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
