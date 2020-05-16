import java.io.*;
import java.net.*;
import java.util.*;

public class NodeUDPListener implements Runnable{

    private PrintWriter out;
    private BufferedReader in;
    private SortedSet<Request> requests, replies;
    private Set<String> peers;
    private DatagramSocket socket;
    private int max_data_chunk = 10 * 1, requestnumber, pdu_size = max_data_chunk + 256;
    private volatile boolean running = true;
    private byte[] buffer = new byte[pdu_size];
    private byte[] pduBuffer = new byte[pdu_size];
    private InetAddress address;
    private Map<String,SortedSet<PDU>> pduPackets; //ainda não está implementado, mas em principio vamos armazenar aqui os pacotes que chegam ao nodo enquando não chegaram todos os seus parceiros


    final String secretKey = "HelpMeObiWanKenobi!";

    public NodeUDPListener(DatagramSocket socket, SortedSet<Request> r, SortedSet<Request> rep, Set<String> ps) {
        try {
            this.socket = socket;
            this.requests = r;
            this.replies = rep;
            this.pduPackets = new HashMap<>();
            this.peers = ps;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        Object o = null;
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            o = is.readObject();
            is.close();
        return o;
    }

    public static byte[] addAll(final byte[] array1, byte[] array2) {
        byte[] joinedArray = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    private boolean allFragments(String id){
        SortedSet<PDU> fragments = pduPackets.get(id);
        PDU p = fragments.first();
        int total = p.getTotal_fragments();
        if (fragments.size() == total)
            return true;
        else return false;
    }

    private void assembler(String id) throws IOException, ClassNotFoundException {
        if (allFragments(id)){
            SortedSet<PDU> fragments = pduPackets.get(id);

            //Alocar um buffer para colocat todos os pdus
            PDU p = fragments.first();
            byte[] buffer = new byte[p.getTotal_fragments()*max_data_chunk];

            int j = 0;
            for (Iterator<PDU> it = fragments.iterator(); it.hasNext(); ) {
                PDU n = it.next();
                System.arraycopy(n.getData(), 0, buffer, j*max_data_chunk, n.getData().length);
                j++;
            }

            Request r = (Request)deserialize(buffer);

            System.out.println("> UDPListener: Converting packet to Request");
            if (r.getStatus(secretKey).equals("na")) {
                requests.add(r);
                System.out.println("> UDPListener: Request added to queue");
            } else if (r.getStatus(secretKey).equals("sd")){
                replies.add(r);
                System.out.println("> UDPListener: Reply added to queue");
            }
        }
    }

    private boolean validOrigin(String s){
        String[] ip = s.split("\\s+");
        if (peers.contains(ip[0]))
            return true;
        else return false;
    }

    public void run() {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (running) {
            try {
                // receber pacotes udp
                socket.receive(packet);
                System.out.println("> UDPListener: Receiving packet");
                pduBuffer = packet.getData();

                System.out.println("> UDPListener: Converting Buffer to PDU");
                PDU pdu = (PDU) deserialize(pduBuffer);
                Arrays.fill(pduBuffer, (byte) 0);

                if (validOrigin(pdu.getIdentifier(secretKey))) {
                    System.out.println("> UDPListener: Packet Received");

                    if (this.pduPackets.containsKey(pdu.getIdentifier(secretKey))) {
                        SortedSet<PDU> fragments = pduPackets.get(pdu.getIdentifier(secretKey));
                        if (!fragments.contains(pdu)) {
                            fragments.add(pdu);
                        }
                        pduPackets.put(pdu.getIdentifier(secretKey), fragments);
                    } else {
                        Comparator comparator = new PDUComparator();
                        SortedSet<PDU> fragments = new TreeSet<>(comparator);
                        fragments.add(pdu);
                        pduPackets.put(pdu.getIdentifier(secretKey), fragments);
                    }

                    assembler(pdu.getIdentifier(secretKey));
                } else {
                    System.out.println("> UDPListener: Packet from unknown source");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
