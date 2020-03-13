import Exceptions.InsufficientParametersException;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Node {
    private InetAddress my_address;
    private Set<InetAddress> peers;
    private InetAddress target_address;
    private int outside_port;
    private int protected_port = 6666;
    private ServerSocket external_socket;
    private DatagramSocket internal_socket_out, internal_socket_in;

    public Node() {

        try {
            this.my_address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        peers = new HashSet<>();
        try {
            internal_socket_in = new DatagramSocket();
            internal_socket_out = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    // Esta função recebe os paramtros da linha de comandos e preenche a estrutura
    // e retorna erro se não forem fornecidos todos os parametros
    public void setupNode(String[] args) throws InsufficientParametersException {

        int set_target_server = 0;
        int set_port = 0;
        int set_peers = 0;

        for (int i = 0; i < args.length; i++){
            if (args[i].equals("target-server")){
                try {
                    target_address = InetAddress.getByName(args[i+1]);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                set_target_server = 1;
            }

            if (args[i].equals("port")){
                outside_port = Integer.parseInt(args[i+1]);
                set_port = 1;
            }

            if (args[i].equals("overlay-peers")){
                for (int j = i+1; j < args.length; j++){
                    try {
                        if (!InetAddress.getByName(args[j]).equals(my_address))
                            peers.add(InetAddress.getByName(args[j]));
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }

                set_peers = 1;
            }
        }

        if (set_target_server + set_port + set_peers < 3 )
            throw new InsufficientParametersException("Insufficient Parameters");
    }

    public void printNodeInfo(){
        System.out.println("Node info:");
        System.out.println("Node Address: " + my_address);
        System.out.print("Node peers: ");
        System.out.println(Arrays.toString(peers.toArray()));
        System.out.println("Target Address: " + target_address);
        System.out.println("Outside Port: " + outside_port);
        System.out.println("Protected Port: " + protected_port);
    }

    // Esta função inicializa o socket tcp na porta definida e cria uma thread para gerir os pedidos que chegam
    public void startTCPSocket() throws IOException {
        external_socket = new ServerSocket(this.outside_port);

        while (true) {
            Socket socket = external_socket.accept();
            NodeWorker nw = new NodeWorker(socket);
            new Thread(nw).start();
        }
    }


}
