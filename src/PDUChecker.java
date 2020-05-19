import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A classe PDUChecker é responsaveç por efetuar os pedidos de retransmissão de fragmentos
 * que possam ter-se perdido ou estar atrasados. Esta classe traduz-se numa thread.
 */
public class PDUChecker implements Runnable{
    /**
     * Contém o identificador relativo ao PDU.
     */
    private String identifier;
    /**
     * Contém todos os ids de PDU's suspeitos de terem algum problema.
     */
    private Set<String> suspects;
    /**
     * Contém o map dos ids de PDU's e dos seus respetivos fragmentos.
     */
    private Map<String,SortedSet<PDU>> pdus;
    /**
     * Contém o boolen que nos permite quebrar a execução.
     */
    private volatile boolean running = true;
    /**
     * Contém o socket udp para envio de PDU's de controlo.
     */
    private DatagramSocket control_socket;
    /**
     * Contém a porta para envio de PDU's de controlo.
     */
    private int control_port;

    /**
     * Contém a chave de desencriptação.
     */
    final String secretKey = "HelpMeObiWanKenobi!";

    /**
     * Construtor para a clasee PDUChecker.
     * @param   ps      map dos ids de PDU's e dos seus respetivos fragmentos
     * @param   s       conjunto de ids suspeitos
     * @param   id      identificador do PDU
     * @param   cport   porta de controlo
     * @param   csock   socket de controlo
     */
    public PDUChecker(Map<String, SortedSet<PDU>> ps, Set<String> s, String id, int cport, DatagramSocket csock) {
        this.identifier = id;
        this.suspects = s;
        this.pdus = ps;
        this.control_port = cport;
        this.control_socket = csock;
    }

    /**
     * Esta função verifica quais são os fragmentos que faltam para que o Request possa ser montado.
     * @return          conjunto dos fragmentos em falta
     */
    private Set<Integer> missingFragments(){
        Set<Integer> present = new HashSet<>();
        Set<Integer> missing = new HashSet<>();
        SortedSet<PDU> fragments = pdus.get(this.identifier);
        PDU p = fragments.first();
        int max = p.getTotal_fragments();

        for (Iterator<PDU> it = fragments.iterator(); it.hasNext(); ) {
            PDU n = it.next();
            present.add(n.getPosition());
        }

        for (int i = 1; i <= max; i++)
            if (!present.contains(i))
                missing.add(i);

        return missing;
    }

    /**
     * Esta função serializa um dado objeto.
     * @param   obj     objeto
     * @return          objeto serializado
     */
    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        os.flush();
        os.close();
        byte[] b = out.toByteArray();
        out.flush();
        out.close();
        return b;
    }

    /**
     * Esta função pede o reenvio dos fragmentos fornecidos.
     * @param   fragments   conjunto com os ids dos fragmentos em falta
     */
    private void resendFragment(Set<Integer> fragments) throws IOException {
        String[] ip = this.identifier.split("\\s+");
        InetAddress address = InetAddress.getByName(ip[0]);

        for (Integer i :fragments ) {
            PDU pdu = new PDU();
            pdu.setIdentifier(identifier, secretKey);
            pdu.setControl(1);
            pdu.setPosition(1);
            pdu.setTotal_fragments(1);
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            pdu.setTimestamp(timestamp.getTime());
            byte[] aux = i.toString().getBytes();
            pdu.setData(aux);
            byte[] pdubuffer = serialize(pdu);
            DatagramPacket packet = new DatagramPacket(pdubuffer, pdubuffer.length, address, this.control_port);
            control_socket.send(packet);
        }
    }

    /**
     * Esta função verifica se estão presentes todos os fragmentos.
     * @param   id   id do PDU em questão
     * @return       valor booleano da resposta
     */
    private boolean allFragments(String id){
        SortedSet<PDU> fragments = pdus.get(id);
        PDU p = fragments.first();
        int total = p.getTotal_fragments();
        if (fragments.size() == total)
            return true;
        else return false;
    }

    /**
     * Ciclo de execução da thread.
     */
    public void run() {
        while (running){
            try {
                if (allFragments(identifier)) {
                    System.out.println("> PDUChecker: All fragments are present");
                    running = false;
                } else {
                    System.out.println("> PDUChecker: Activated");
                    Set<Integer> missing = missingFragments();
                    System.out.println("> PDUChecker: Checking for missing fragments");
                    resendFragment(missing);
                    System.out.println("> PDUChecker: Asked for resend");

                    TimeUnit.MILLISECONDS.sleep(100);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        System.out.println("> PDUChecker: Deactivated");
    }
}
