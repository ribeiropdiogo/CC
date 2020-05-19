import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Classe encarregue do atedimentos de pedidos aplicacionais
 * por meios de TCP. Implementa Runabble pois deve estar sempre
 * à escuta de novos pedidos, sendo por isso uma componente de thread por si 
 * própria.
 */
public class NodeTCPListener implements Runnable {

    /**
     * Define o endereço do destino.
     */
    private String target_address;

    /**
     * Define o endereço do nodo que requesitou o listener.
     */
    private String node_address;

    /**
     * Define o endereço da origem de todos os pedidos escutados.
     */
    private String source_address;

    /**
     * Define o socket que permite a comunicação da aplicação original com o nodo.
     */
    private Socket socket;

    /**
     * Socket que define a comunição de PDUs entre nodos da rede.
     */
    private DatagramSocket UDPsocket;

    /**
     * Socket reservado à comunicação de controlo de erros.
     */
    private DatagramSocket  control_socket;

    /**
     * Fila de espera com todos os pedidos a serem processados.
     */
    private SortedSet<Request> requests;

    /**
     * Fila de espera com todas as repostas ainda não submetidas.
     */
    private SortedSet<Request> replies;

    /**
     * Indica o status do nodo atual. true se estiver em uso, false caso contrário.
     */
    private Boolean running;

    /**
     * Indica todos os overlay-pears de confiança desta rede.
     */
    private Set<String> peers;

    /**
     * Define a lista de espera de todos as origens que estão em espera para ser atendidas.
     * Este mecanismo permite obter uma súbtil forma de controlo contra ataques de negação de serviço.
     */
    private List<String> waitinglist;

    /**
     * Define a porta reservada a comunicação de PDUs entre nós da rede AnonGW.
     */
    private int protected_port;
    
    /**
     * Indica, a cada instante, o numero de PDUs que já foram servidos por aquela rede.
     */
    private int requestn;
    
    /**
     * Porta reservada à comunicação de controlo de erros.
     */
    private int control_port;

    /**
     * Chave encriptação, utilizada sobre todos os conteudos inseridos dentro da rede.
     */
    final String secretKey = "HelpMeObiWanKenobi!";

    /**
     * Inicializa o TCP Listener com todas as variaveis necessárias ao seu funcionamento.
     * É importante denotar que os argumentos utilizados representam na verdade objetos partilhados entres
     * todas as diversas entidades do sistema.
     */
    public NodeTCPListener(Socket s, SortedSet<Request> r, SortedSet<Request> rep, String address, String naddress, DatagramSocket usocket, Set<String> p, int port, int served, int control_port, DatagramSocket control_socket) {
        this.socket = s;
        this.requests = r;
        this.target_address = address;
        this.node_address = naddress;
        this.running = true;
        this.UDPsocket = usocket;
        this.peers = p;
        this.protected_port = port;
        this.waitinglist =  new ArrayList<>();
        this.replies = rep;
        this.requestn = served;
        this.control_port = control_port;
        this.control_socket = control_socket;
    }

    /**
     * Gerar um numero pertencente ao intervalo discreto entre lower e upper bound.
     *
     * @param lower Define o lower bound necessário.
     * @param upper Define o upper bound necessário.
     *
     * @return Um número pertencente ao intervalo discreto [lower,upper].
     */
    private int random(int lower, int upper){
        return  (int) (Math.random() * (upper - lower)) + lower;
    }

    /**
     * Permite calcular o enderçeo aleatoria de um overlay-peer, de forma a servir
     * o pedido a este mesmo peer.
     *
     * @return O endereço de um overlay-peer.
     *
     * @see random
     */
    private String getPeer(){
        int i = random(0,peers.size());
        String[] ps = peers.toArray(new String[peers.size()]);
        return ps[i];
    }

    /**
     * Verifica se já existe alguma pedido proveniente do endereço indicado, ainda em espera.
     *
     * @param sourceAddress Endereço que se pretende verificar.
     *
     * @return True se já existir um pedido daquela origem, false caso contrário.
     */
    private boolean repeatedRequest(String sourceAddress) {
        boolean r = false;

        if (waitinglist.contains(sourceAddress))
            r = true;

        return r;
    }

    /**
     * Inicializa a thread responsável pelo handling dos pedidos.
     *
     * @param s Define o socket para comunição de pedidos para a rede AnonGW.
     * @param r Pedido a ser processador.
     * @param port Porta pela qual o pedido foi atendido.
     *
     * @throws IOException Erro de I/O relacionado com threads.
     */
    public void startRequestHandler(DatagramSocket s,Request r, int port) throws IOException {

        Thread handler = new Thread(){
            public void run(){
                    try {
                        RequestHandler rh = new RequestHandler(s,r,getPeer(),port,requestn,node_address,control_socket,control_port);
                        new Thread(rh).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }
        };
        handler.start();
    }

    /**
     * Inicializa a thread responsável por fornecer respostas aos clientes TCP.
     *
     * @param out Indica o buffer de escrita alocado ao cliente.
     * @param client Indica o cliente ao qual pretendemos fornecer uma respostas.
     *
     * @throws IOException Erro de I/O relacionado com threads.
     */
    public void startTCPReplier(BufferedWriter out, String client) throws IOException {

        Thread listener = new Thread(){
            public void run(){
                try {
                    NodeTCPReplier nl = new NodeTCPReplier(socket,replies,out,client);
                    new Thread(nl).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        listener.start();
    }

    /**
     * Executa o listener, de forma a que este esteja sempre à espera de novos pedidos.
     */
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(socket.getOutputStream()), "UTF-8"));

            final String data = in.readLine();
            System.out.println("> TCPListener: Established new connection with outside");
                if (!socket.getRemoteSocketAddress().toString().equals(target_address)) {
                    final Request r = new Request(this.node_address,socket.getRemoteSocketAddress().toString().substring(1),secretKey);
                    String clientaddress = socket.getRemoteSocketAddress().toString().substring(1);
                    r.setMessage(data,secretKey);
                    r.setContactNodeAddress(this.node_address,secretKey);

                    System.out.println("> TCPListener: Created the new Request");
                    if (!repeatedRequest(socket.getRemoteSocketAddress().toString().substring(1))) {
                        this.waitinglist.add(socket.getRemoteSocketAddress().toString().substring(1));
                        
                        out.write("");
                        startRequestHandler(this.UDPsocket,r,this.protected_port);
                        System.out.println("> TCPListener: Sent the new Request");

                        startTCPReplier(out,clientaddress);

                    }
                }

            System.out.println("> TCPListener: Job Ended");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
