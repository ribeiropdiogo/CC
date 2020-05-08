import java.util.concurrent.TimeUnit;
import Exceptions.*;
import Model.Node;

public class AnonGW {

    private static Node node1;
    private static Node node2;

    public static void main(String[] args) {
	System.out.println("Coisas");
        node1 = new Node();
        node2 = new Node();
        try {
            node1.setupNode(args);
            node1.printNodeInfo();
            System.out.println("TCP Listener Start");
            node1.startTCPListener();
            System.out.println("TCP Speaker Start");
            node1.startTCPSpeaker();


            /**
             *  O MÉTODO DE COMUNICAÇÃO SERÁ ALGUMA COISA DESTE GÉNERO
             */

            /*node1.setupNode(args);
            node1.printNodeInfo();
            node2.setupNode(args);
            node2.printNodeInfo();
            System.out.println("TCP Listener Start");
            node1.startTCPListener();
            System.out.println("UDP Speaker Start");
            node2.startUDPListener();
            System.out.println("UDP Listener Start");
            node2.startUDPListener();
            System.out.println("TCP Speaker Start");
            node2.startTCPSpeaker();*/

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
