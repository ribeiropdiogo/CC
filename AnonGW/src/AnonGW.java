import Exceptions.InsufficientParametersException;

public class AnonGW {

    private static Node node;

    public static void main(String[] args) {
        node = new Node();
        try {
            node.setupNode(args);
            node.printNodeInfo();
            node.startTCPSocket();
        } catch (Exception e){
            System.out.println(e);
        }

    }
}
