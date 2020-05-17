import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RequestHandler implements Runnable{
    private DatagramSocket internal_socket, control_socket;
    private String peer, nodeadress;
    private Request request;
    private int protected_port, control_port;
    private volatile boolean running = true;
    private SortedSet<PDU> fragments;
    private int max_data_chunk = 20 * 1024, requestnumber, pdu_size = max_data_chunk + 256;
    private byte[] controlbuffer = new byte[pdu_size], pducontrolbuffer = new byte[pdu_size], pduBuffer = new byte[pdu_size];
    private  Map<Integer,byte[]> pdufragments;

    //controlo de pacotes
    //private int protected_control_port = 8888;
    //private DatagramSocket internal_control_socket;
    //private byte[] pduBuffer = new byte[pdu_size];
    //fim

    final String secretKey = "HelpMeObiWanKenobi!";

    public RequestHandler(DatagramSocket socket, Request r, String peer, int port, int requestn, String node, DatagramSocket csort, int cport){
        this.internal_socket = socket;
        this.request = r;
        this.peer = peer;
        this.protected_port = port;
        this.nodeadress = node;
        this.requestnumber = requestn;
        this.pdufragments = new HashMap<>();
        this.control_port = cport;
        this.control_socket = csort;
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

    private static Object deserialize(byte[] data) {
        Object o = null;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            o = is.readObject();
            is.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return o;
    }

    private void sendFragment(String identifier, int j, int i, int real_length, InetAddress address, byte[] buffer) throws IOException {
        PDU pdu = new PDU();
        pdu.setIdentifier(identifier,secretKey);
        pdu.setControl(0);
        pdu.setPosition(j+1);
        pdu.setTotal_fragments(i);
        pdu.setTotalSize(real_length);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        pdu.setTimestamp(timestamp.getTime());
        byte[] aux = new byte[max_data_chunk];
        int tam = 0;

        int remain = buffer.length - j*max_data_chunk;
        if (remain < max_data_chunk)
            tam = remain;
        else  tam = max_data_chunk;

        System.arraycopy(buffer, j*max_data_chunk, aux, 0, tam);
        pdu.setData(aux);

        //Pdu para bytes
        byte[] pdubuffer = serialize(pdu);
        pdufragments.put(j+1,pdubuffer);

        System.out.println("PDU info:");
        System.out.println("PDU size: "+pdubuffer.length);
        System.out.println("id: "+pdu.getIdentifier(secretKey));
        System.out.println("control: "+pdu.getControl());
        System.out.println("fragments: "+pdu.getTotal_fragments());
        System.out.println("position: "+pdu.getPosition());
        System.out.println("datasize: "+pdu.getData().length);

        //Enviar o PDU
        DatagramPacket packet = new DatagramPacket(pdubuffer, pdubuffer.length, address, this.protected_port);
        internal_socket.send(packet);
    }

    public void run() {
        while (running) {
            System.out.println("> Launched RequestHandler");
            try {
                String identifier = nodeadress + " " + requestnumber;
                //Converter Request em Bytes
                byte[] buffer = serialize(request);
                int real_length = buffer.length;
                InetAddress address = null;
                address = InetAddress.getByName(peer);

                //pegar em pedaços do buffer e criar PDU's
                float f = ((float)buffer.length/(float)max_data_chunk);
                int i = (int)Math.ceil(f);


                for (int j = 0;j < i;j++){
                    sendFragment(identifier,j,i,real_length,address,buffer);
                }

                boolean end = false;

                DatagramPacket packet = new DatagramPacket(controlbuffer, controlbuffer.length);
                while (!end){
                    control_socket.receive(packet);
                    pduBuffer = packet.getData();
                    PDU pdu = (PDU) deserialize(pduBuffer);
                    Arrays.fill(pduBuffer, (byte) 0);
                    if (pdu.getIdentifier(secretKey).equals(identifier)){
                        String msg = new String(pdu.getData());
                        if (msg.equals("success")){
                            end = true;
                            System.out.println("> RequestHandler: Received success message ");
                        } else {
                            int fragment = Integer.parseInt(new String(pdu.getData()));
                            sendFragment(identifier,fragment,i,real_length,address,pdufragments.get(fragment));
                            System.out.println("> RequestHandler: Resending fragment " + fragment);
                        }
                    }
                }

                System.out.println("> RequestHandler: Sent Request to peer "+address);
                running = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}