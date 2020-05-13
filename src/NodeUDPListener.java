import java.io.*;
import java.net.*;
import java.util.*;

public class NodeUDPListener implements Runnable{

    private PrintWriter out;
    private BufferedReader in;
    private SortedSet<Request> requests, replies;
    private Set<String> peers;
    private DatagramSocket socket;
    private volatile boolean running = true;
    private byte[] buffer = new byte[20*1024];
    private byte[] requestBuffer = new byte[20*1024];
    private InetAddress address;
    private Map<String,SortedSet<PDU>> pduPackets; //ainda não está implementado, mas em principio vamos armazenar aqui os pacotes que chegam ao nodo enquando não chegaram todos os seus parceiros
    private int max_data_chunk = 50 * 1024, requestnumber, pdu_size = max_data_chunk + 256;

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

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public static byte[] addAll(final byte[] array1, byte[] array2) {
        byte[] joinedArray = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    public void run() {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (running) {
            try {
                // receber pacotes udp
                socket.receive(packet);
                System.out.println("> UDPListener: Receiving packet");
                requestBuffer = packet.getData();


                //O QUE EU FIZ - DESCOMENTA ISTO PARA VER O QUE EU FIZ E COMENTA A PARTE QUE ESTAVA ANTES

                /*
                //Adiciona pdu ao map
                System.out.println("> UDPListener: Converting Buffer to PDU");
                PDU pdu = new PDU();
                pdu = (PDU)deserialize(requestBuffer);
                Arrays.fill(requestBuffer, (byte)0);
                System.out.println("> UDPListener: Packet Received");

                //Verifica se é um PDU de controlo
                if(pdu.getControl() == 1) {

                    //Se sim, temos que inicializar o RequestHandler e enviar o pacote que falta;
                    System.out.println("> UDPListener: Error Control Packet Received");

                } else {

                    SortedSet<PDU> pduS = pduPackets.get(pdu.getIdentifier(secretKey));

                    // if sorted set does not exist create it
                    if(pduS == null) {
                        Comparator comparator = new PDUComparator();
                        pduS = new TreeSet<>(comparator); // Eu fiz em forma de SortedSet, mas ainda temos que fazer um comparador decente
                        pduS.add(pdu);
                        pduPackets.put(pdu.getIdentifier(secretKey),pduS);
                    } else {
                        //add pdu if its not in list
                        if (!pduS.contains(pdu)) pduS.add(pdu);
                    }

                    //Já recebemos os pacotes todos por isso podemos montar o request
                    if(pdu.getTotal_fragments() == pduS.size()) {

                        byte[] start = new byte[0];

                        for (Iterator<PDU> it = pduS.iterator(); it.hasNext(); ) {
                            PDU p = it.next();
                            byte[] newb = p.getData();
                            start = addAll(start,newb);
                        }

                        Request r = (Request)deserialize(start);

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
                */


                //Ainda temos que mudar isto. Temos que ir buscar os pdus ao map quando eles já estiveram lá todos para converter para Request



                // COMO ESTAVA ANTES - COMENTA ISTO PARA VERES COMO EU FIZ

                ///*
                System.out.println("> UDPListener: Packet Received");
                // colocar esses pacotes udp na fila de espera
                Request r = (Request)deserialize(requestBuffer);
                Arrays.fill(requestBuffer, (byte)0);
                System.out.println("> UDPListener: Converting packet to Request");
                if (r.getStatus(secretKey).equals("na")) {
                    requests.add(r);
                    System.out.println("> UDPListener: Request added to queue");
                } else if (r.getStatus(secretKey).equals("sd")){
                    replies.add(r);
                    System.out.println("> UDPListener: Reply added to queue");
                }
                //*/

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
