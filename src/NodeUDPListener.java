import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Classe responsavel por ficar a escrut a de novos pedidos UDP.
 */
public class NodeUDPListener implements Runnable{

    /**
     * Permite materializar um escrito para o processamento de resultado formatado.
     */
    private PrintWriter out;

    /**
     * Buffer de leitura, por parte de outas componentes.
     */
    private BufferedReader in;

    /**
     * Fila de espera com todos os pedidos a serem processados.
     */
    private SortedSet<Request> requests;

    /**
     * Fila de espera com todas as repostas ainda não submetidas.
     */
    private SortedSet<Request> replies;

    /**
     * Indica todos os overlay-pears de confiança desta rede.
     */
    private Set<String> peers;

    /**
     * Define o socket que permite a comunicação da aplicação original com o nodo.
     */
    private DatagramSocket socket;

    /**
     * Define o tamanho maximo do campo data dos PDUs.
     */
    private int max_data_chunk = 20 * 1;

    /**
     * Define o numero do pedido a ser processado.
     */
    private int requestnumber;

    /**
     * Max PDU size.
     */
    private int  pdu_size = max_data_chunk + 256;

    /**
     * Porta reservada à comunicação de controlo de erros.
     */
    private int  control_port;

    /**
     * Indica o status do nodo atual. true se estiver em uso, false caso contrário.
     */
    private volatile boolean running = true;

    /**
     * Buffer auxiliar ao processamento da porção de data do PDU.
     */
    private byte[] buffer = new byte[pdu_size];

    /**
     * Buffer para o processamento serializado do PDU.
     */
    private byte[] pduBuffer = new byte[pdu_size];

    /**
     * Define o endereço do nó, permite conhecer quem está a transmitir a mensagem.
     */
    private InetAddress address;

    /**
     * Define uma catagolação de PDUs, associando a cada endereço de entrada, uma respectiva fila
     * de PDUs a serem processados.
     */
    private Map<String,SortedSet<PDU>> pduPackets;

    /**
     * Ligações que se suspeite que tenham resultado num timeout.
     */
    private Set<String> suspects;

    /**
     * Conjunto das ligações que já foram servidas.
     */
    private Set<String> served;

    /**
     * Socket reservado à comunicação de controlo de erros.
     */
    private DatagramSocket control_socket;

    /**
     * Chave encriptação, utilizada sobre todos os conteudos inseridos dentro da rede.
     */
    final String secretKey = "HelpMeObiWanKenobi!";

    /**
     * Inicializa o UDP Listener com todas as variaveis necessárias ao seu funcionamento.
     * É importante denotar que os argumentos utilizados representam na verdade objetos partilhados entres
     * todas as diversas entidades do sistema.
     */
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

    /**
     * Permite serializar um objeto, para que este possa ser processado como um conjunto de bytes.
     *
     * @param obj Objecto a ser serializado.
     *
     * @return O conjunto de bytes representativo.
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
     * Permite deserializar um conjunto de bytes e transformar no respectivo objeto.
     *
     * @param data Conjunto de bytes representiativos.
     * 
     * @return Objeto deserializado.
     */
    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        Object o = null;
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        o = is.readObject();
        is.close();
        return o;
    }

    /**
     * Verifica se todos os fragmentos daquele PDU já foram comunicados.
     *
     * @param id Id do PDU a ser verificado.
     *
     * @return True se todos os fragmentos ja tiverem sido enviados.
     */
    private boolean allFragments(String id){
        SortedSet<PDU> fragments = pduPackets.get(id);
        PDU p = fragments.first();
        int total = p.getTotal_fragments();
        if (fragments.size() == total)
            return true;
        else return false;
    }

    /**
     * Materializa no PDU especificado, uma mensagem de sucesso de envio deste mesmo pacote.
     *
     * @param identifier Identificador do PDU.
     */
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

    /**
     * Reconstori em memoria um PDU, consoante todos os fragmentos a ele associados.
     *
     * @param id Identificador do PDU que se pretende reconstruir.
     */
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
            if (suspects.contains(id))
                suspects.remove(id);
            System.out.println("> UDPListener: Reply added to queue");
        }
    }

    /**
     * Verifica se o PDU está presa a tentar comunicar algum pacote, o que pode resultar de timeout.
     *
     * @param id PDU que se pretende verificar.
     */
    public boolean stalled(String id){
        SortedSet<PDU> fragments = pduPackets.get(id);
        PDU p = fragments.first();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long current = timestamp.getTime();
        long creationTime = p.getTimestamp();
        long dif = current - creationTime;
        if (dif > 3500) {
            return true;
        }
        else return false;
    }

    /**
     * Mecanismo para evitar a eventualidade de timeouts.
     */
    public void anti_stall() throws IOException {

        Thread handler = new Thread(){
            public void run(){
                while (running) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Set<String> s = pduPackets.keySet();

                    for (String id : s)
                        if (stalled(id) && !suspects.contains(id) && !served.contains(id)) {
                            System.out.println("> UDPListener: Anti-Stall mode");
                            suspects.add(id);
                            try {
                                startPDUChecker(id);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                }
            }
        };
        handler.start();
    }

    /**
     * Inicializa uma thread responsavel pela garantia de entrega do pacote.
     *
     * @param id Parametro que se pretende garantir.
     */
    public void startPDUChecker(String id) throws IOException {

        Thread handler = new Thread(){
            public void run(){
                try {
                    PDUChecker pc = new PDUChecker(pduPackets, suspects, id, control_port, control_socket);
                    new Thread(pc).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        handler.start();
    }

    /**
     * Permite a reconstrução global do pacote indicado, com a garantir de que o pacote ja foi totalmente recebido.
     */
    private void assembler(String id) throws IOException, ClassNotFoundException {
        if (allFragments(id))
            assemble(id);
    }

    /**
     * Verifica se a origem indicada corresponde a uma origem valida.
     */
    private boolean validOrigin(String s){
        String[] ip = s.split("\\s+");
        if (peers.contains(ip[0]))
            return true;
        else return false;
    }

    /**
     * Executa o listener, de forma a que este esteja sempre à espera de novos pedidos.
     */
    public void run() {
        try {
            anti_stall();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
