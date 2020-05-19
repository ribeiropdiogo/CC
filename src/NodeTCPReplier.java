import java.io.*;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Classe repsonsável por fornecer respostas aos clientes TCP.
 */
public class NodeTCPReplier implements Runnable {

    /**
     * Define o socket que permite a comunicação da aplicação original com o nodo.
     */
    private Socket socket;

    /**
     * Fila de espera com todas as repostas ainda não submetidas.
     */
    private SortedSet<Request> replies;

    /**
     * Indica o status do nodo atual. true se estiver em uso, false caso contrário.
     */
    private Boolean running = true;

    /**
     * Define o endereço do cliente ao qual estamos a responder de momento.
     */
    private String clientaddress;

    /**
     * Define o buffer de escrita para comunicação com o client.
     */
    private BufferedWriter out;

    /**
     * Chave encriptação, utilizada sobre todos os conteudos inseridos dentro da rede.
     */
    final String secretKey = "HelpMeObiWanKenobi!";

    /**
     * Inicializa o TCP Replier, com todas as variaveis necessarias ao seu funcionamento.
     * Os objetos indicados rpresentam objetos partilhados entre todas as diversas entidades do sistema.
     */
    public NodeTCPReplier(Socket s, SortedSet<Request> r, BufferedWriter out, String client) {
        this.socket = s;
        this.replies = r;
        this.clientaddress = client;
        this.out = out;
    }

    /**
     * Executa o replier, de forma a que este esteja sempre a responder a clientes.
     */
    public void run() {
        try {
            while (running) {
                try {
                    TimeUnit.MILLISECONDS.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (replies.size() > 0) {
                    Request requesttoserve = replies.first();
                    for (Iterator<Request> it = replies.iterator(); it.hasNext(); ) {
                        Request f = it.next();
                        if (f.getOrigin_address(secretKey).equals(clientaddress))
                            requesttoserve = f;
                    }

                    if(requesttoserve.getOrigin_address(secretKey).equals(clientaddress)) {
                        try {
                            if (requesttoserve.getStatus(secretKey).equals("sd")) {
                                System.out.println("> TCPReplier: Request has been served at destination!");
                                requesttoserve.setStatus("to", secretKey);

                                //Envia a resposta
                                Object[] rarray = requesttoserve.getResponse(secretKey);
                                for (Object s : rarray)
                                    out.write(s.toString());

                                out.flush();

                                requesttoserve.setStatus("so",secretKey);
                                System.out.println("> TCPReplier: Request has been served at origin!");

                                replies.remove(requesttoserve);
                                System.out.println("> TCPReplier: Request has been removed from Queue!");

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        running = false;
                    }
                }
            }

            socket.close();
            out.flush();
            System.out.println("> TCPReplier: Job Ended");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
