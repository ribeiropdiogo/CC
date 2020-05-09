package review;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

public class NodeUDPSpeaker implements Runnable{

    private PrintWriter out;
    private BufferedReader in;
    private SortedSet<Request> requests;
    private Set<String> peers;
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];
    private InetAddress address;
    private int port;

    public NodeUDPSpeaker(SortedSet<Request> r, Set<String> p) {
        this.requests = r;
        this.peers = p;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        running = true;

        while (running) {
            for (Iterator<Request> it = this.requests.iterator(); it.hasNext(); ) {
                Request r = it.next();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = null;
                try {
                    try {
                        out = new ObjectOutputStream(bos);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.writeObject(r);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    buf = bos.toByteArray();
                    for (Iterator<String> it2 = this.peers.iterator(); it2.hasNext(); ) {
                        String i = it2.next();
                        try {
                            address = InetAddress.getByName(i);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } finally {
                    try {
                        bos.close();
                    } catch (IOException ex) {
                        // ignore close exception
                    }
                }
            }
        }
        socket.close();
    }
}