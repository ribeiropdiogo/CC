import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.SortedSet;

public class RequestHandler implements Runnable{
    private DatagramSocket internal_socket;
    private String peer, nodeadress;
    private Request request;
    private int protected_port;
    private volatile boolean running = true;
    //private SortedSet<PDU> storage;
    private int max_data_chunk = 50 * 1024, requestnumber, pdu_size = max_data_chunk + 256;

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
        os.close();
        byte[] b = out.toByteArray();
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

                    //Adicionar pdu ao armazem no caso de falhar algum pacote
                    //storage.add(pdu);
                    //Pdu para bytes
                    byte[] pdubuffer = serialize(pdu);

                    PDU r = (PDU) deserialize(pdubuffer);
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

                System.out.println("> RequestHandler: Sent Request to peer "+address);
                running = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
