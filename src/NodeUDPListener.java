import java.io.*;
import java.net.*;
import java.util.*;

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
    private int max_data_chunk = 50 * 1024, requestnumber, pdu_size = max_data_chunk + 256;

    final String secretKey = "HelpMeObiWanKenobi!";

    public NodeUDPListener(DatagramSocket socket, SortedSet<Request> r, SortedSet<Request> rep) {
        try {
            this.socket = socket;
            this.requests = r;
            this.replies = rep;
            this.pduPackets = new HashMap<>();
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

                //Adiciona pdu ao map
                System.out.println("> UDPListener: Converting Buffer to PDU");
                PDU pdu = new PDU();
                pdu = (PDU)deserialize(requestBuffer);
                Arrays.fill(requestBuffer, (byte)0);

                SortedSet<PDU> pduS = pduPackets.get(pdu.getIdentifier(secretKey));

                // if sorted set does not exist create it
                if(pduS == null) {
                    Comparator comparator = new PDUComparator();
                    pduS = new TreeSet<>(comparator); // Eu fiz em forma de SortedSet, mas ainda temos que fazer um comparador decente
                    pduS.add(pdu);
                    pduPackets.put(pdu.getIdentifier(secretKey),pduS);
                } else {
                    //add pdu if its not in list
                    if (!pduS.contains(pdu)) pduS.add(pdu);
                }

                //Verificar se já temos os pacotes todos


                //Ainda temos que mudar isto. Temos que ir buscar os pdus ao map quando eles já estiveram lá todos para converter para Request

                System.out.println("> UDPListener: Packet Received");
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
