import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RequestHandler implements Runnable{
    private DatagramSocket internal_socket;
    private String peer;
    private Request request;
    private int protected_port;
    private volatile boolean running = true;

    public RequestHandler(DatagramSocket socket, Request r, String peer, int port){
        this.internal_socket = socket;
        this.request = r;
        this.peer = peer;
        this.protected_port = port;
    }

    public void run() {
        while (running) {
            System.out.println("> Launched RequestHandler");
            try {
                byte[] buffer = new byte[20 * 1024];
                InetAddress address = null;
                address = InetAddress.getByName(peer);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, this.protected_port);
                internal_socket.send(packet);
                System.out.println("> RequestHandler: Sent Request to peer");
                running = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
