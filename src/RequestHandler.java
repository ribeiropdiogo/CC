import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class RequestHandler implements Runnable{
    private DatagramSocket internal_socket;
    private String peer, nodeadress;
    private Request request;
    private int protected_port;
    private volatile boolean running = true;
    private SortedSet<PDU> fragments;
    private int max_data_chunk = 10 * 1, requestnumber, pdu_size = max_data_chunk + 256;

    final String secretKey = "HelpMeObiWanKenobi!";

    public RequestHandler(DatagramSocket socket, Request r, String peer, int port, int requestn, String node){
        this.internal_socket = socket;
        this.request = r;
        this.peer = peer;
        this.protected_port = port;
        this.nodeadress = node;
        this.requestnumber = requestn;
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

    private void controlFlow(){
        Comparator comparator = new PDUComparator();
        fragments = new TreeSet<>(comparator);
    }

    /*
    private boolean controlPacketReceiver() {

    }
     */

    /*
    private void controlPacketSender(String identifier,InetAddress address) throws IOException {
        PDU pdu = new PDU();
        pdu.setIdentifier(identifier,secretKey);
        pdu.setControl(1);
        pdu.setPosition(0);
        pdu.setTotal_fragments(0);
        pdu.setTotalSize(0);
        byte[] aux = new byte[0];
        pdu.setData(aux);

        //Pdu para bytes
        byte[] pdubuffer = serialize(pdu);

        //Enviar o PDU
        DatagramPacket packet = new DatagramPacket(pdubuffer, pdubuffer.length, address, this.protected_port);
        internal_socket.send(packet);
    }
    */

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
                controlFlow();

                //pegar em pedaços do buffer e criar PDU's
                float f = ((float)buffer.length/(float)max_data_chunk);
                int i = (int)Math.ceil(f);

                //System.out.println(">: "+i+" "+buffer.length+" "+max_data_chunk+" | "+f+" | "+Math.ceil(f));

                for (int j = 0;j < i;j++){
                    PDU pdu = new PDU();
                    pdu.setIdentifier(identifier,secretKey);
                    pdu.setControl(0);
                    pdu.setPosition(j+1);
                    pdu.setTotal_fragments(i);
                    pdu.setTotalSize(real_length);
                    byte[] aux = new byte[max_data_chunk];
                    int tam = 0;

                    int remain = buffer.length - j*max_data_chunk;
                    if (remain < max_data_chunk)
                        tam = remain;
                    else  tam = max_data_chunk;

                    System.arraycopy(buffer, j*max_data_chunk, aux, 0, tam);
                    pdu.setData(aux);


                    fragments.add(pdu);

                    //Pdu para bytes
                    byte[] pdubuffer = serialize(pdu);

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

                // VAMOS ENVIAR O PACOTE DE CONTROLO ENQUANTO NÃO RECEBERMOS RESPOSTA

                /*
                do {
                    controlPacketSender(identifier,address,this.protected_port);
                } while(!controlPacketReceiver);
                 */

                System.out.println("> RequestHandler: Sent Request to peer "+address);
                running = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

/*
                PDU pdu = new PDU();
                pdu.setIdentifier(identifier,secretKey);
                pdu.setControl(0);
                pdu.setPosition(1);
                pdu.setTotal_fragments(1);
                pdu.setTotalSize(buffer.length);
                pdu.setData(buffer);
                byte[] pdubuffer = serialize(pdu);
                System.out.println("PDU info:");
                System.out.println("PDU size: "+pdubuffer.length);
                System.out.println("id: "+pdu.getIdentifier(secretKey));
                System.out.println("control: "+pdu.getControl());
                System.out.println("fragments: "+pdu.getTotal_fragments());
                System.out.println("position: "+pdu.getPosition());
                System.out.println("datasize: "+pdu.getData().length);
                DatagramPacket packet = new DatagramPacket(pdubuffer, pdubuffer.length, address, this.protected_port);
                internal_socket.send(packet);
 */