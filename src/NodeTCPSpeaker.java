import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

/**
 * Classe responsável pela comunicação de todas as ligações
 * que chegam a um determinado por via TCP, para a sua conversa em comunicação
 * por UDP.
 */
public class NodeTCPSpeaker implements Runnable {

    /**
     * Define o socket que permite a comunicação da aplicação original com o nodo.
     */
    private Socket external_socket_out;

    /**
     * Permite materializar um escrito para o processamento de resultado formatado.
     */
    private PrintWriter out;

    /**
     * Buffer de leitura, por parte de outas componentes.
     */
    private BufferedReader in;

    /**
     * Porta de comunicação com a aplicação, usada para falar com o servidor. Por exemplo,
     * no caso de usamos wget, utilizariamos a outside_port 80, que é a porta usada por essa ferramenta.
     */
    private int outside_port;

    /**
     * Fila de espera com todos os pedidos a serem processados.
     */
    private SortedSet<Request> requests;

    /**
     * Define o endereço do destino.
     */
    private String target_address;

    /**
     * Define a porta reservada a comunicação de PDUs entre nós da rede AnonGW.
     */
    private int protected_port;

    /**
     * Porta reservada à comunicação de controlo de erros.
     */
    private int control_port;

    /**
     * Indica o status do nodo atual. true se estiver em uso, false caso contrário.
     */
    private volatile boolean running = true;

    /**
     * Contador utilizado internamente para o funcionamento do nodo.
     */
    private int contador = 0;

    /**
     * Socket que define a comunição de PDUs entre nodos da rede.
     */
    private DatagramSocket UDPsocket;

    /**
     * Socket reservado à comunicação de controlo de erros.
     */
    private DatagramSocket control_socket;

    /**
     * Chave encriptação, utilizada sobre todos os conteudos inseridos dentro da rede.
     */
    final String secretKey = "HelpMeObiWanKenobi!";

    /**
     * Inicializa o TCP Speaker com todas as variaveis necessárias ao seu funcionamento.
     * É importante denotar que os argumentos utilizados representam na verdade objetos partilhados entres
     * todas as diversas entidades do sistema.
     */
    public NodeTCPSpeaker(int s, SortedSet<Request> r, String target, int port, DatagramSocket socket, int cport, DatagramSocket csock) {
        this.outside_port = s;
        this.requests = r;
        this.target_address = target;
        this.protected_port = port;
        this.UDPsocket = socket;
        this.control_port = cport;
        this.control_socket = csock;
    }

    /**
     * Inicializa a thread responsável pelo handling dos pedidos.
     *
     * @param s Define o socket para comunição de pedidos para a rede AnonGW.
     * @param r Pedido a ser processador.
     * @param address Define o endereço origem do pedido.
     *
     * @throws IOException Erro de I/O relacionado com threads.
     */
    public void startRequestHandler(DatagramSocket s, Request r, int j, String address) throws IOException {

        Thread handler = new Thread(){
            public void run(){
                Socket socket = null;
                try {
                    RequestHandler rh = new RequestHandler(s,r,r.getContactNodeAddress(secretKey),protected_port,j,address,control_socket,control_port);
                    new Thread(rh).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        handler.start();
    }

    /**
     * Executa o speaker, de forma a que este esteja sempre a processar novos pedidos.
     */
    public void run() {
        try {
            int i = 0;
            while (running) {
                try {
                    TimeUnit.MILLISECONDS.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (requests.size() > 0) {
                    external_socket_out = new Socket(target_address, outside_port);
                    Request r = requests.first();
                    
                    if (r.getStatus(secretKey).equals("na")) {
                        r.setStatus("ad",secretKey);
                        System.out.println("> Speaker: Found request!");


                        System.out.println("> TCPSpeaker: Sent request to server");
                        // Envia o pedido ao servidor de destino
                        PrintWriter pw = new PrintWriter(external_socket_out.getOutputStream());
                        pw.println(r.getMessage(secretKey));
                        pw.println();
                        pw.flush();


                        System.out.println("> TCPSpeaker: Getting response from server");
                        // Recebe a resposta do servidor de destino
                        BufferedReader br = new BufferedReader(new InputStreamReader(external_socket_out.getInputStream()));
                        String t;
                        while ((t = br.readLine()) != null)
                            r.concatenateResponse(t,secretKey);
                        r.setStatus("sd",secretKey);

                        System.out.println("> TCPSpeaker: Request has been served at destination!");
                        br.close();

                        external_socket_out.close();

                        //enviar o request via udp de volta
                        String ip = InetAddress.getLocalHost().getHostAddress();
                        i++;
                        startRequestHandler(this.UDPsocket,r,i,ip);

                        //remover o request da fila de espera deste nodo
                        requests.remove(r);
                    }
                }

            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
