import Exceptions.InsufficientParametersException;

import java.util.concurrent.TimeUnit;

public class AnonGW {

    private static Node node;

    public static void main(String[] args) {
        node = new Node();
        try {
            node.setupNode(args);
            node.printNodeInfo();
            System.out.println("Listener Start");
            node.startTCPListener();
            System.out.println("Speaker Start");
            node.startTCPSpeaker();

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
