public class AnonGW {

    private static Node node;

    public static void main(String[] args) {
	System.out.println("> AnonGW started");
        node = new Node();
        try {
            node.setupNode(args);
            node.printNodeInfo();
            System.out.println("> Launched TCPListener");
            node.startTCPListener();
            System.out.println("> Launched TCPSpeaker");
            node.startTCPSpeaker();
            System.out.println("> Launched UDPListener");
            node.startUDPListener();
            /*
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
