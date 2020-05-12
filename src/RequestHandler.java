import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
    private SortedSet<PDU> storage;
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
        return out.toByteArray();
    }

    public void run() {
        while (running) {
            System.out.println("> Launched RequestHandler");
            try {
                String identifier = nodeadress + " " + requestnumber;
                //Converter Request em Bytes
                byte[] buffer = serialize(request);

                InetAddress address = null;
                address = InetAddress.getByName(peer);

                //pegar em pedaços do buffer e criar PDU's
                float f = buffer.length/max_data_chunk;
                int i = (int)Math.ceil(f);

                System.out.println(">: "+i+" "+buffer.length+" "+max_data_chunk+" | "+f+" | "+Math.ceil(f));
                for (int j = 0;j < i;j++){
                    PDU pdu = new PDU();
                    pdu.setIdentifier(identifier,secretKey);
                    pdu.setControl(0);
                    pdu.setPosition(j+1);
                    pdu.setTotal_fragments(i);
                    byte[] aux = new byte[max_data_chunk];
                    System.arraycopy(buffer, 0, aux, 0, max_data_chunk);
                    pdu.setData(aux);

                    //Adicionar pdu ao armazem no caso de falhar algum pacote
                    storage.add(pdu);


                    //Pdu para bytes
                    byte[] pdubuffer = serialize(pdu);
                    //Enviar o PDU

                    System.out.println("PDU info:");
                    System.out.println("id: "+pdu.getIdentifier(secretKey));
                    System.out.println("control: "+pdu.getControl());
                    System.out.println("fragments: "+pdu.getTotal_fragments());
                    System.out.println("position: "+pdu.getPosition());
                    System.out.println("datasize: "+pdu.getData().length);

                    DatagramPacket packet = new DatagramPacket(pdubuffer, pdu_size, address, this.protected_port);
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
