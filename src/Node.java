import Exceptions.InsufficientParametersException;

import java.io.*;
import java.net.*;
import java.util.*;

public class Node {

    private String my_address;
    private Set<String> peers;
    private String target_address;
    private int outside_port;
    private int protected_port = 6666;
    private ServerSocket external_socket_in;
    private Socket external_socket_out;
    private DatagramSocket internal_socket;
    private SortedSet<Request> requests, replies;

    /* CONSTRUTORES */

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

    public void printNodeInfo() {
        System.out.println("Node info:");
        System.out.println("Node Address: " + my_address);
        System.out.print("Node peers: ");
        System.out.println(Arrays.toString(peers.toArray()));
        System.out.println("Target Address: " + target_address);
        System.out.println("Outside Port: " + outside_port);
        System.out.println("Protected Port: " + protected_port);
    }

    // Esta função inicializa o socket tcp na porta definida e cria uma thread para gerir os pedidos que chegam
    public void startTCPListener() throws IOException {
        external_socket_in = new ServerSocket(this.outside_port);

        Thread listener = new Thread(){
            public void run(){
                while (true) {
                    Socket socket = null;
                    try {
                        socket = external_socket_in.accept();
                        NodeTCPListener nl = new NodeTCPListener(socket, requests,target_address, my_address, internal_socket, peers,protected_port);
                        new Thread(nl).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        listener.start();
    }


    // Esta função é usada para o nó comunicar com o servidor de destino
    public void startTCPSpeaker() {
        Thread speaker = new Thread(){
            public void run(){

                    try {
                        NodeTCPSpeaker ns = new NodeTCPSpeaker(outside_port, requests, target_address,protected_port,internal_socket);
                        new Thread(ns).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

        };

        speaker.start();
    }

    /*
    // Só funciona quando o TCPListener estiver ativado
    // Esta função é usada para o nó comunicar com o segundo nó destino
    public void startUDPSpeaker() throws IOException {
        Thread uspeaker = new Thread() {
            public void run() {
                try {
                    NodeUDPSpeaker nus = new NodeUDPSpeaker(requests, peers);
                    new Thread(nus).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        uspeaker.start();
    }
    */

    // Esta função é usada para o nó ler a comunicação vinda do primeiro nó
    public void startUDPListener() throws IOException{
        Thread ulistener = new Thread(){
            public void run(){
                try {
                    NodeUDPListener nul = new NodeUDPListener(internal_socket,requests,replies);
                    new Thread(nul).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        ulistener.start();
    }


    // so para testes
    public void queuesize() {
        System.out.println("uelele: " + this.requests.size());
    }
}
