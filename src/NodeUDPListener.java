import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;

public class NodeUDPListener implements Runnable{

    private PrintWriter out;
    private BufferedReader in;
    private SortedSet<Request> requests, replies;
    private Set<String> peers;
    private DatagramSocket socket;
    private int max_data_chunk = 20 * 1, requestnumber, pdu_size = max_data_chunk + 256, control_port;
    private volatile boolean running = true;
    private byte[] buffer = new byte[pdu_size];
    private byte[] pduBuffer = new byte[pdu_size];
    private InetAddress address;
    private Map<String,SortedSet<PDU>> pduPackets;
    private Set<String> suspects, served;
    private DatagramSocket control_socket;

    final String secretKey = "HelpMeObiWanKenobi!";

    public NodeUDPListener(DatagramSocket socket, SortedSet<Request> r, SortedSet<Request> rep, Set<String> ps, int control_port, DatagramSocket control_socket) {
        try {
            this.socket = socket;
            this.requests = r;
            this.replies = rep;
            this.pduPackets = new HashMap<>();
            this.peers = ps;
            this.suspects = new HashSet<>();
            this.served = new HashSet<>();
            this.control_socket = control_socket;
            this.control_port = control_port;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        os.flush();
        os.close();
        byte[] b = out.toByteArray();
        out.flush();
        out.close();
        return b;
    }

    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        Object o = null;
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        o = is.readObject();
        is.close();
        return o;
    }

    private boolean allFragments(String id){
        SortedSet<PDU> fragments = pduPackets.get(id);
        PDU p = fragments.first();
        int total = p.getTotal_fragments();
        if (fragments.size() == total)
            return true;
        else return false;
    }

    private void successMessage(String identifier) throws IOException {
        PDU pdu = new PDU();
        pdu.setIdentifier(identifier, secretKey);
        pdu.setControl(1);
        pdu.setPosition(1);
        pdu.setTotal_fragments(1);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        pdu.setTimestamp(timestamp.getTime());
        String msg = "success";
        byte[] aux = msg.getBytes();
        pdu.setData(aux);
        byte[] pdubuffer = serialize(pdu);
        String[] ip = identifier.split("\\s+");
        InetAddress address = InetAddress.getByName(ip[0]);
        DatagramPacket packet = new DatagramPacket(pdubuffer, pdubuffer.length, address, control_port);
        control_socket.send(packet);
    }

    public void assemble(String id) throws IOException, ClassNotFoundException{
        SortedSet<PDU> fragments = pduPackets.get(id);

        //Alocar um buffer para colocat todos os pdus
        PDU p = fragments.first();
        byte[] buffer = new byte[p.getTotal_fragments() * max_data_chunk];

        int j = 0;
        for (Iterator<PDU> it = fragments.iterator(); it.hasNext(); ) {
            PDU n = it.next();
            System.arraycopy(n.getData(), 0, buffer, j * max_data_chunk, n.getData().length);
            j++;
        }

        Request r = (Request) deserialize(buffer);

        System.out.println("> UDPListener: Converting packet to Request");
        successMessage(id);
        if (r.getStatus(secretKey).equals("na")) {
            requests.add(r);
            System.out.println("> UDPListener: Request added to queue");
        } else if (r.getStatus(secretKey).equals("sd")) {
            replies.add(r);
            served.add(id);
            System.out.println("> UDPListener: Reply added to queue");
        }
    }

    public boolean stalled(String id){
        SortedSet<PDU> fragments = pduPackets.get(id);
        PDU p = fragments.first();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long current = timestamp.getTime();
        long creationTime = p.getTimestamp();

        if (current - creationTime > 3500) {
            System.out.println("> UDPListener: Stalled");
            return true;
        }
        else return false;
    }

    public void startPDUChecker(String id) throws IOException {

        Thread handler = new Thread(){
            public void run(){
                try {
                    PDUChecker pc = new PDUChecker(pduPackets, suspects, id);
                    new Thread(pc).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        handler.start();
    }

    private void assembler(String id) throws IOException, ClassNotFoundException {
        if (allFragments(id)) {
            assemble(id);
        } else {
            if (stalled(id)) {
                this.suspects.add(id);
                startPDUChecker(id);
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

                if (validOrigin(pdu.getIdentifier(secretKey)) && !served.contains(pdu.getIdentifier(secretKey))) {
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
