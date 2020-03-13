import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class NodeWorker implements Runnable{
    private Socket socket;

    public NodeWorker(Socket s) {
        this.socket = s;
    }

    public void run() {
        System.out.println("> New connection");
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

            String data = in.readLine();

            while (data != null) {
                System.out.println(data);

                // Tratar os pedidos que recebe

            }

            socket.close();
            out.flush();
            System.out.println("> Connection ended");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
