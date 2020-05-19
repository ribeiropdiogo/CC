/**
 * Classes principal, portadora da função main, e responsável por efetuar
 * toda a devida inicialização de parametro, bem como a inicialização de
 * todos os listeners e speakers.
 */
public class AnonGW {

    /**
     * Variavel de instância que define a estrutura atomica, sobre a qual
     * todos os metodos são devidamente aplicados em todas as suas formas.
     */
    private static Node node;

    /**
     * Permite fornecer um start point à aplicação, responsavel por fazer toda
     * a devida inicialização da rede de acordo com os argumentos transmitidos.
     *
     * @param args Argumentos de inicialização da rede.
     */
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

        } catch (Exception e){
            System.out.println(e);
        }

    }
}
