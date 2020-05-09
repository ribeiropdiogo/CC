import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;

public class NodeUDPListener implements Runnable{

    private PrintWriter out;
    private BufferedReader in;
    private SortedSet<Request> requests;
    private Set<String> peers;
    private DatagramSocket socket;
    private volatile boolean running = true;
    private byte[] buffer = new byte[20*1024];
    private byte[] requestBuffer = new byte[20*1024];
    private InetAddress address;

    public NodeUDPListener(DatagramSocket socket, SortedSet<Request> r) {
        try {
            this.socket = socket;
            this.requests = r;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public void run() {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (running) {
            try {
                // receber pacotes udp
                socket.receive(packet);
                System.out.println("> UDPListener: Receiving packet");
                requestBuffer = packet.getData();


                System.out.println("> UDPListener: packet received");
                // colocar esses pacotes udp na fila de espera
                Request r = (Request)deserialize(requestBuffer);
                Arrays.fill(requestBuffer, (byte)0);
                System.out.println("> UDPListener: Converting packet to Request");
                requests.add(r);
                System.out.println("> UDPListener: Request added to queue");
            } catch (Exception e) {
                e.printStackTrace();
            }



            /*
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
            */
        }
    }
}
