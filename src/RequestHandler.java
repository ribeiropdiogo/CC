import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RequestHandler implements Runnable{
    private DatagramSocket internal_socket;
    private String peer, nodeadress;
    private Request request;
    private int protected_port;
    private volatile boolean running = true;
    private SortedSet<PDU> fragments;
    private int max_data_chunk = 10 * 10, requestnumber, pdu_size = max_data_chunk + 256;
    private byte[] controlbuffer = new byte[pdu_size], pducontrolbuffer = new byte[pdu_size], buffer = new byte[pdu_size], pduBuffer = new byte[pdu_size];

    private int control_port = 8989;
    private DatagramSocket control_socket;
    private  Map<Integer,byte[]> pdufragments;

    //controlo de pacotes
    //private int protected_control_port = 8888;
    //private DatagramSocket internal_control_socket;
    //private byte[] pduBuffer = new byte[pdu_size];
    //fim

    final String secretKey = "HelpMeObiWanKenobi!";

    public RequestHandler(DatagramSocket socket, Request r, String peer, int port, int requestn, String node){
        this.internal_socket = socket;
        this.request = r;
        this.peer = peer;
        this.protected_port = port;
        this.nodeadress = node;
        this.requestnumber = requestn;
        this.pdufragments = new HashMap<>();
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

    /*
    private void controlFlow(){
        Comparator comparator = new PDUComparator();
        fragments = new TreeSet<>(comparator);
    }
    private int controlPacketReceiver(int[] positionsr) {
        try {
            internal_control_socket = new DatagramSocket(protected_control_port);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            // receber pacotes udp
            internal_control_socket.receive(packet);
            System.out.println("> RequestHandler: Receiving control packet");
            pduBuffer = packet.getData();
            System.out.println("> RequestHandler: Converting Buffer to PDU");
            PDU pdu = (PDU) deserialize(pduBuffer);

            //Se recebermos um pacote com control == 2, então

            if(pdu.getControl()==2) {

                System.out.println("> Request Handler: Finito");
                return 2;

            } else {

                Arrays.fill(pduBuffer, (byte) 0);
                byte[] data = pdu.getData();
                String pos = (String) deserialize(data);
                positionsr = new int[pdu.getTotal_fragments()];
                int j = 0;
                //tirar as positions da string
                for(int i = 0; i < pos.length(); i++) {
                    if(pos.charAt(i)!='-') {
                        positionsr[j] = pos.charAt(i);
                        j++;
                    }
                }

                //imprimir as positions que temos de reenviar
                System.out.println(positionsr);

                return 1;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
    private int controlPacketSender(String identifier,InetAddress address) throws IOException {
        try {
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

            return 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
    private int repeatPacketSender(String identifier,InetAddress address, int[] positionv) throws IOException {
        try {

            for (Iterator<PDU> it = fragments.iterator(); it.hasNext(); ) {
                PDU n = it.next();
                for(int i = 0; i < positionv.length; i++) {
                    if(n.getPosition() == positionv[i]) {
                        //Pdu para bytes
                        byte[] pdubuffer = serialize(n);

                        //Enviar o PDU
                        DatagramPacket packet = new DatagramPacket(pdubuffer, pdubuffer.length, address, this.protected_port);
                        internal_socket.send(packet);
                    }
                }
            }

            return 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
    */

    private void sendFragment(String identifier, int j, int i, int real_length, InetAddress address) throws IOException {
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
                //controlFlow();

                //pegar em pedaços do buffer e criar PDU's
                float f = ((float)buffer.length/(float)max_data_chunk);
                int i = (int)Math.ceil(f);

                //System.out.println(">: "+i+" "+buffer.length+" "+max_data_chunk+" | "+f+" | "+Math.ceil(f));

                for (int j = 0;j < i;j++){
                    sendFragment(identifier,j,i,real_length,address);
                }

                boolean end = false;

                this.control_socket = new DatagramSocket(control_port);

                DatagramPacket packet = new DatagramPacket(controlbuffer, controlbuffer.length);
                while (!end){
                    control_socket.receive(packet);
                    pduBuffer = packet.getData();
                    PDU pdu = (PDU) deserialize(pduBuffer);
                    Arrays.fill(pduBuffer, (byte) 0);
                    if (pdu.getIdentifier(secretKey).equals(identifier)){
                        String msg = pdu.getData().toString();
                        if (msg.equals("success")){
                            end = true;
                            System.out.println("> RequestHandler: Received success message ");
                        } else {
                            int fragment = Integer.parseInt(pdu.getData().toString());
                            sendFragment(identifier,fragment,i,real_length,address);
                            System.out.println("> RequestHandler: Resending fragment " + fragment);
                        }
                    }
                }

                // 1º ESPERAMOS UNS SEGUNDOS

                // DEPOIS ESTAMOS SEMPRE A ENVIAR A MENSAGEM DE CONTROLO ENQUANTO NAO RECEBERMOS UM PEDIDO DE PACOTES OU UM PACOTE COM CONTROL == 2 A CONFIRMAR QUE ESTA TUDO PRONTO

                // REPEAT PACKET SENDER ENVIA OS PACOTES PEDIDOS

                // CONTROL PACKET SENDER MANDA UM PACOTE DE CONTROLO A PERGUNTAR SE ESTA TUDO BEM

                // CONTROLPACKETRECEIVER ESTA SEMPRE A LER OS PACOTES QUE ESTAO A CHEGAR ATE FICARMOS COM CONTROLD == 2
                /*
                TimeUnit.SECONDS.sleep(2);

                int[] positionsr = null;
                int controld = 0;

                do {
                    if(controld == 1) {
                        controld = repeatPacketSender(identifier,address,positionsr);
                    } else {
                        controld = controlPacketSender(identifier, address);
                    }
                    controld = controlPacketReceiver(positionsr);
                } while(controld!=2);

                */

                System.out.println("> RequestHandler: Sent Request to peer "+address);
                running = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}