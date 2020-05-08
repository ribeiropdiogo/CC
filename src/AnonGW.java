import java.util.concurrent.TimeUnit;
import Exceptions.*;
import Model.Node;

public class AnonGW {

    private static Node node;

    public static void main(String[] args) {
	System.out.println("Coisas");
        node = new Node();
        try {
            node.setupNode(args);
            node.printNodeInfo();
            System.out.println("TCP Listener Start");
            node.startTCPListener();
            System.out.println("TCP Speaker Start");
            node.startTCPSpeaker();
            System.out.println("UDP Listener Start");
            node.startUDPListener();
            System.out.println("UDP Speaker Start");
            node.startUDPSpeaker();


            /*
            while (true){
                TimeUnit.SECONDS.sleep(5);
                node.queuesize();
            }*/

        } catch (Exception e){
            System.out.println(e);
        }

    }
}
