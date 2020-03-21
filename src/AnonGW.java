import Exceptions.InsufficientParametersException;

import java.util.concurrent.TimeUnit;

public class AnonGW {

    private static Node node;

    public static void main(String[] args) {
        node = new Node();
        try {
            node.setupNode(args);
            node.printNodeInfo();
            node.startTCPListener();
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
