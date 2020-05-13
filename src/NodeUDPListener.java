import java.io.*;
import java.net.*;
import java.util.*;

public class NodeUDPListener implements Runnable{

    private PrintWriter out;
    private BufferedReader in;
    private SortedSet<Request> requests, replies;
    private Set<String> peers;
    private DatagramSocket socket;
    private int max_data_chunk = 10 * 1024, requestnumber, pdu_size = max_data_chunk + 256;
    private volatile boolean running = true;
    private byte[] buffer;
    private byte[] pduBuffer;
    private InetAddress address;
    private Map<String,SortedSet<PDU>> pduPackets; //ainda não está implementado, mas em principio vamos armazenar aqui os pacotes que chegam ao nodo enquando não chegaram todos os seus parceiros


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
            PDU p = fragments.first();
            byte[] buffer = new byte[p.getTotalSize()];
            System.arraycopy(p.getData(), 0, buffer, 0, p.getData().length);
            int j = 1;
            for (Iterator<PDU> it = fragments.iterator(); it.hasNext(); ) {
                PDU n = it.next();
                System.arraycopy(n.getData(), 0, buffer, j*max_data_chunk, n.getData().length);
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

    public void run() {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (running) {
            try {
                // receber pacotes udp
                socket.receive(packet);
                System.out.println("> UDPListener: Receiving packet");
                pduBuffer = packet.getData();

                //Adiciona pdu ao map
                System.out.println("> UDPListener: Converting Buffer to PDU - "+pduBuffer.length);
                PDU pdu = new PDU();
                pdu = (PDU)deserialize(pduBuffer);
                Arrays.fill(pduBuffer, (byte)0);

                System.out.println("> UDPListener: Packet Received");

                if (this.pduPackets.containsKey(pdu.getIdentifier(secretKey))){
                    SortedSet<PDU> fragments = pduPackets.get(pdu.getIdentifier(secretKey));
                    if (!fragments.contains(pdu))
                        fragments.add(pdu);
                    pduPackets.put(pdu.getIdentifier(secretKey),fragments);
                } else {
                    Comparator comparator = new PDUComparator();
                    SortedSet<PDU> fragments = new TreeSet<>(comparator);
                    fragments.add(pdu);
                    pduPackets.put(pdu.getIdentifier(secretKey),fragments);
                }

                assembler(pdu.getIdentifier(secretKey));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
