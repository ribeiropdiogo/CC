import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/**
 * A classe RequestHandler é aquela que é responsável por enviar um Request via udp para
 * um nodo da mesma rede. Esta classe traduz-se numa thread.
 */
public class RequestHandler implements Runnable{

    /**
     * Contém os sockets UDP para o envio de PDU's.
     */
    private DatagramSocket internal_socket, control_socket;
    /**
     * Contém os endereços do peer e do nó que pretende enviar o Request.
     */
    private String peer, nodeadress;
    /**
     * Contém o Request a enviar.
     */
    private Request request;
    /**
     * Contém as portas para o envio de PDU's.
     */
    private int protected_port, control_port;
    /**
     * Contém o boolen que nos permite quebrar a execução.
     */
    private volatile boolean running = true;
    /**
     * Contém os fragmentos para enviar.
     */
    private SortedSet<PDU> fragments;
    /**
     * Contém os valores do tamanho máximo do payload, o número do Request, e o tamanho máximo do PDU.
     */
    private int max_data_chunk = 20 * 1, requestnumber, pdu_size = max_data_chunk + 256;
    /**
     * Contém os arrays para guardar os PDU's em bytes.
     */
    private byte[] controlbuffer = new byte[pdu_size], pducontrolbuffer = new byte[pdu_size], pduBuffer = new byte[pdu_size];
    /**
     * Contém o map que corresponde o número do fragmento aos bytes crrespondentes.
     */
    private  Map<Integer,byte[]> pdufragments;

    /**
     * Contém a chave para a encriptação.
     */
    final String secretKey = "HelpMeObiWanKenobi!";

    /**
     * Construtor para a clasee RequestHandler.
     * @param   socket      socket UDP
     * @param   r           Request
     * @param   peer        peer de destino
     * @param   port        porta de envio
     * @param   requestn    número do Request
     * @param   node        endereço do nodo de origem
     * @param   csort       socket de controlo
     * @param   cport       porta de controlo
     */
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

    /**
     * Esta função serializa um dado objeto.
     * @param   obj     objeto
     * @return          objeto serializado
     */
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

    /**
     * Esta função deserializa um dado array de bytes.
     * @param   data    objeto em bytes
     * @return          objeto deserializado
     */
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
    /**
     * Esta função envia um PDU para o destino via UDP.
     * @param   identifier    identificador do PDU
     * @param   j             numero do fragmento
     * @param   i             total de fragmentos
     * @param   real_length   tamanho real do Request
     * @param   address       endereço de destino
     * @param   buffer        PDU serializado
     */
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

        System.out.println("-------------------------------");
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

    /**
     * Ciclo de execução da thread.
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
                            sendFragment(identifier,fragment-1,i,real_length,address,buffer);
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