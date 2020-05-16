import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;

public class PDUChecker implements Runnable{
    private String identifier;
    private Set<String> suspects;
    private Map<String,SortedSet<PDU>> pdus;
    private volatile boolean running = true;
    private DatagramSocket control_socket;
    private int control_port = 8989;

    final String secretKey = "HelpMeObiWanKenobi!";

    public PDUChecker(Map<String, SortedSet<PDU>> ps, Set<String> s, String id) {
        this.identifier = id;
        this.suspects = s;
        this.pdus = ps;
        try {
            this.control_socket = new DatagramSocket(control_port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private Set<Integer> missingFragments(){
        Set<Integer> present = new HashSet<>();
        Set<Integer> missing = new HashSet<>();
        SortedSet<PDU> fragments = pdus.get(this.identifier);
        PDU p = fragments.first();
        int max = p.getTotal_fragments();

        for (Iterator<PDU> it = fragments.iterator(); it.hasNext(); ) {
            PDU n = it.next();
            present.add(n.getPosition());
        }

        for (int i = 1; i <= max; i++)
            if (!present.contains(i))
                missing.add(i);

        return missing;
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

    private void resendFragment(Set<Integer> fragments) throws IOException {
        String[] ip = this.identifier.split("\\s+");
        InetAddress address = InetAddress.getByName(ip[0]);

        for (Integer i :fragments ) {
            PDU pdu = new PDU();
            pdu.setIdentifier(identifier, secretKey);
            pdu.setControl(1);
            pdu.setPosition(1);
            pdu.setTotal_fragments(1);
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            pdu.setTimestamp(timestamp.getTime());
            byte[] aux = i.toString().getBytes();
            pdu.setData(aux);
            byte[] pdubuffer = serialize(pdu);

            DatagramPacket packet = new DatagramPacket(pdubuffer, pdubuffer.length, address, this.control_port);
            control_socket.send(packet);
        }
    }

    private boolean allFragments(String id){
        SortedSet<PDU> fragments = pdus.get(id);
        PDU p = fragments.first();
        int total = p.getTotal_fragments();
        if (fragments.size() == total)
            return true;
        else return false;
    }

    public void run() {
        while (running){
            try {
                Set<Integer> missing = missingFragments();
                resendFragment(missing);
                if (allFragments(identifier)) {
                    suspects.remove(identifier);
                    running = false;
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
