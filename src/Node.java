import Exceptions.InsufficientParametersException;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Classe responsável pela gestão e armazenamento de todas as sub-componentes
 * da rede AnonGw, bem como a sua respectiva inteligação.
 */
public class Node {

    /**
     * Define o endereço do nó de entrada, permite conhecer quem está a transmitir a mensagem.
     */
    private String my_address;

    /**
     * Armazena todos os overlay-peers conectados, peers estes pela qual a rede ira desseminar informação.
     */
    private Set<String> peers;

    /**
     * Define o ponto de destino, de facto, para a mensagem que se pretende transmitir.
     */
    private String target_address;

    /**
     * Porta de comunicação com a aplicação, usada para falar com o servidor. Por exemplo,
     * no caso de usamos wget, utilizariamos a outside_port 80, que é a porta usada por essa ferramenta.
     */
    private int outside_port;

    /**
     * Indica, a cada instante, o numero de PDUs que já foram servidos por aquela rede.
     */
    private int served = 0;

    /**
     * Porta utilizada para comunicação de PDUs entre nós da rede AnonGW.
     */
    private int protected_port = 6666;

    /**
     * Porta utilizada para controlo de erros decorrentes da comunicação entre
     * nós da rede, de forma a não perturbar as normais comunicações de PDUs pela
     * porta predefinida.
     */
    private int control_port = 4646;

    /**
     * Socket utilizado para a transmissão de informação da aplicação para o nodo.
     */
    private ServerSocket external_socket_in;

    /**
     * Socket utilizado para a transmissão de informação do nodo para a aplicação.
     */
    private Socket external_socket_out;

    /**
     * Socket interno que permite a comunicação e debate de PDUs entre os nós da rede.
     */
    private DatagramSocket internal_socket;

    /**
     * Socket reservado à comunicação de erros decorrentes da comunicação entre nõs da rede.
     */
    private DatagramSocket control_socket;

    /**
     * Usado com o objetivo de emular uma fila de espera, representativa de todos os pedidos
     * que estão para ser processados pela rede, mas que, devido a fatores interno, ainda não
     * o foram.
     */
    private SortedSet<Request> requests;

    /**
     * Fila de espera com todos os pedidos já respondido pela rede e que estão a espera de ser
     * retransmitidos para a sua devida aplicação, com a resposta necessaria já fornecida.
     */
    private SortedSet<Request> replies;

    /**
     * Construtor inial da nodo, permite popular todos os campos necessários. Com uma especial
     * enfase na população do endereço de nó, bem como a inicialização de sockets internos para
     * para comunicação inter-nodal.
     */
    public Node() {

        try {
            this.my_address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        peers = new HashSet<>();
        Comparator comparator = new RequestComparator();
        requests = new TreeSet(comparator);
        replies = new TreeSet(comparator);

        try {
            internal_socket = new DatagramSocket(protected_port);
            control_socket = new DatagramSocket(control_port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Efetuada a inicialização deste nodo principal, de acordo com os respectivos argumentos
     * populando, inteligentemente, todos os dados do nodo.
     *
     * @param args Dados, passados como argumento da aplicação inicial, de inicialização.
     * 
     * @throws InsufficientParametersException Sempre que o nodo pondere insuficientes os parametros fornecidos, deve ser emitido um erro.
     */
    public void setupNode(String[] args) throws InsufficientParametersException {

        int set_target_server = 0;
        int set_port = 0;
        int set_peers = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("target-server")) {
                try {
                    target_address = InetAddress.getByName(args[i+1]).getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                set_target_server = 1;
            }

            if (args[i].equals("port")) {
                outside_port = Integer.parseInt(args[i+1]);
                set_port = 1;
            }

            if (args[i].equals("overlay-peers")) {
                for (int j = i+1; j < args.length; j++) {
                    try {
                        if (!InetAddress.getByName(args[j]).getHostAddress().equals(my_address))
                            peers.add(InetAddress.getByName(args[j]).getHostAddress());
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
                set_peers = 1;
            }
        }

        if (set_target_server + set_port + set_peers < 3 ) {
            throw new InsufficientParametersException("Insufficient Parameters");
        }
    }

    /**
     * Imprime toda a informação sobre o nodo que acabo de ser criado, o que é de extrema importãncia
     * para garantir uma coerente comunicação com o utilizador da aplicação em questão. Sendo que este
     * está presente em todos os passos principais, é apenas correto permitir que este obtenha informação
     * sobre a qual está a transmitir a sua informação.
     */
    public void printNodeInfo() {
        System.out.println("Node info:");
        System.out.println("Node Address: " + my_address);
        System.out.print("Node peers: ");
        System.out.println(Arrays.toString(peers.toArray()));
        System.out.println("Target Address: " + target_address);
        System.out.println("Outside Port: " + outside_port);
        System.out.println("Protected Port: " + protected_port);
    }

    /**
     * Inicializa o socket TCP na porta definida, e cria um thread dedicada para o despacho de pedidos que
     * cheguem à rede.
     *
     * @throws IOException Erro de I/O lançado devido a Threads.
     */
    public void startTCPListener() throws IOException {
        external_socket_in = new ServerSocket(this.outside_port);

        Thread listener = new Thread(){
            public void run(){
                while (true) {
                    Socket socket = null;
                    try {
                        socket = external_socket_in.accept();
                        served++;
                        NodeTCPListener nl = new NodeTCPListener(socket, requests,replies,target_address, my_address, internal_socket, peers,protected_port,served,control_port,control_socket);
                        new Thread(nl).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        listener.start();
    }

    /**
     * Permite a comunicação com o servidor destino desta aplicação.
     */
    public void startTCPSpeaker() {
        Thread speaker = new Thread(){
            public void run(){

                    try {
                        NodeTCPSpeaker ns = new NodeTCPSpeaker(outside_port, requests, target_address,protected_port,internal_socket,control_port,control_socket);
                        new Thread(ns).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

        };

        speaker.start();
    }

    /**
     * Metódo utilizado para a criação de uma thread dedicada à leitura de mensagens provenientes
     * de nos vizinhos da rede AnonGW.
     *
     * @throws IOException Erro de I/O lançado devido a Threads.
     */
    public void startUDPListener() throws IOException {
        Thread ulistener = new Thread(){
            public void run(){
                try {
                    NodeUDPListener nul = new NodeUDPListener(internal_socket,requests,replies,peers,control_port,control_socket);
                    new Thread(nul).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        
        ulistener.start();
    }

}
