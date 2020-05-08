package Model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.SortedSet;

public class NodeUDPSpeaker implements Runnable{

    private PrintWriter out;
    private BufferedReader in;
    private int outside_port;
    private SortedSet<Request> requests;
    private String target_address;
    private int contador = 0;

    final String secretKey = "HelpMeObiWanKenobi!";

    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];

    public NodeUDPSpeaker(int s, SortedSet<Request> r, String target) {
        this.outside_port = s;
        this.requests = r;
        this.target_address = target;
        try {
            socket = new DatagramSocket(4445);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        running = true;

        while (running) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            //Request r = new Request(packet.getData(), 0, packet.getLength());
            Request r = new Request("fdsafsa","fsadfasf");

            if (r.equals("end")) {
                running = false;
                continue;
            }
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }
}