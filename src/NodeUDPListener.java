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
    private int max_data_chunk = 10 * 10, requestnumber, pdu_size = max_data_chunk + 256;
    private volatile boolean running = true;
    private byte[] buffer = new byte[pdu_size];
    private byte[] pduBuffer = new byte[pdu_size];
    private InetAddress address;
    private Map<String,SortedSet<PDU>> pduPackets;
    private Set<String> suspects, served;
    private DatagramSocket control_socket;
    private int control_port = 10808;

    //controlo de pacotes
    private int protected_control_port = 10808;
    private DatagramSocket internal_control_socket;
    //fim

    final String secretKey = "HelpMeObiWanKenobi!";

    public NodeUDPListener(DatagramSocket socket, SortedSet<Request> r, SortedSet<Request> rep, Set<String> ps) {
        try {
            this.socket = socket;
            this.requests = r;
            this.replies = rep;
            this.pduPackets = new HashMap<>();
            this.peers = ps;
            this.suspects = new HashSet<>();
            this.served = new HashSet<>();
            this.control_socket = new DatagramSocket(control_port);
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

    public static byte[] addAll(final byte[] array1, byte[] array2) {
        byte[] joinedArray = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    private void checkMissingPositions(String id,int[] missingP){
        SortedSet<PDU> fragments = pduPackets.get(id);
        PDU p = fragments.first();
        int total = p.getTotal_fragments();
        missingP = new int[total];
        for(int i = 0; i < total; i++) {
            missingP[i] = i+1;
        }
        for (Iterator<PDU> it = fragments.iterator(); it.hasNext(); ) {
            PDU n = it.next();
            for(int i = 0; i < total; i++) {
                if(n.getPosition() == missingP[i]) {
                    missingP[i] = 0;
                }
            }
        }
    }

    private void sendControlPackages(String id,int[] missingP){
        try {
            PDU pdu = new PDU();
            pdu.setIdentifier(id,secretKey);
            pdu.setControl(1);
            pdu.setPosition(0);
            pdu.setTotal_fragments(0);
            pdu.setTotalSize(0);
            byte[] aux = new byte[max_data_chunk];
            int tam = 0;

            String spositions = "";

            StringBuilder sb = new StringBuilder(spositions);

            int j = 0;

            for(int i = 0; i < missingP.length; i++) {
                if(missingP[i]!=0) {
                    sb.insert(j,(char)missingP[i]);
                }
                j++;
                sb.insert(j,'-');
                j++;
            }

            byte[] buffer = serialize(spositions);


            System.arraycopy(buffer, max_data_chunk, aux, 0, max_data_chunk);
            pdu.setData(aux);


            byte[] pdubuffer = serialize(pdu);

            internal_control_socket = new DatagramSocket(protected_control_port);
            DatagramPacket packet = new DatagramPacket(pdubuffer, pdubuffer.length, address, this.protected_control_port);
            internal_control_socket.send(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendOkControlPackages(String id){
        try {
            PDU pdu = new PDU();
            pdu.setIdentifier(id,secretKey);
            pdu.setControl(2);
            pdu.setPosition(0);
            pdu.setTotal_fragments(0);
            pdu.setTotalSize(0);
            byte[] aux = new byte[0];


            byte[] pdubuffer = serialize(pdu);

            internal_control_socket = new DatagramSocket(protected_control_port);
            DatagramPacket packet = new DatagramPacket(pdubuffer, pdubuffer.length, address, this.protected_control_port);
            internal_control_socket.send(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
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

        DatagramPacket packet = new DatagramPacket(pdubuffer, pdubuffer.length, address, this.control_port);
        control_socket.send(packet);
    }

    public void assemble(String id) throws IOException, ClassNotFoundException{
        //sendOkControlPackage(id);
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
        if (r.getStatus(secretKey).equals("na")) {
            requests.add(r);
            System.out.println("> UDPListener: Request added to queue");
        } else if (r.getStatus(secretKey).equals("sd")) {
            replies.add(r);
            successMessage(id);
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

        if (current - creationTime > 3500)
            return true;
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
            /*
            int[] missingP = null;
            checkMissingPositions(id,missingP);
            sendControlPackage(id,missingP);

             */
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

                    /*

                    if(pdu.getControl()==0) {
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

                    } else {
                        assembler(pdu.getIdentifier(secretKey));
                    }

                     */

                } else {
                    System.out.println("> UDPListener: Packet from unknown source");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
