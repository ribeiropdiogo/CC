import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.SortedSet;

public class NodeUDPListener implements Runnable{

    private PrintWriter out;
    private BufferedReader in;
    private SortedSet<Request> requests;
    private Set<String> peers;
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];
    private InetAddress address;
    private int port;

    public NodeUDPListener(String my_address) {
        try {
            try {
                socket = new DatagramSocket(4445,InetAddress.getByName(my_address));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
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
            ByteArrayInputStream bis = new ByteArrayInputStream(buf);
            ObjectInput in = null;
            try {
                try {
                    in = new ObjectInputStream(bis);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Object o = in.readObject();
                    Request r = (Request) o;
                    if (buf.length == 0) {
                        running = false;
                        continue;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    // ignore close exception
                }
            }
        }
        socket.close();
    }
}
