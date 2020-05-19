import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A classe Request é aquela que representa um pedido dentro do nosso sistema. Qualquer pedido
 * é transformado num Request que garante a sua anonimização. Também contém mecanismos que nos
 * permitem ter uma noção de como o Request se encontra dentro do sistema através do seu estado.
 */
public class Request implements Serializable {

    /**
     * Contém a data de criação do Request.
     */
    private long creationTime;
    /**
     * Contém o endereço de origem encriptado.
     */
    private String origin_address;
    /**
     * Contém o endereço do nó que criou o Request.
     */
    private String contact_node_address;
    /**
     * Contém a mensagem que a origem quer transmitir ao destino.
     */
    private String message;
    /**
     * Contém a resposta do servidor à origem, por linhas.
     */
    private List<String> response;
    /**
     * Contém o estado do Request, sendo que os estados possíveis
     * são: na (não atendido), ad (atendido no destino), sd (servido
     * no destino), to (a ser transmitido à origem), so (servido na
     * origem e tbd (to be deleted).
     */
    private String status;

    /**
     * Contrutor da classe Request.
     * @param   node_address    endereço do nó que cria o Request
     * @param   address         endereço da origem
     * @param   secretKey       chave para encriptação
     */
    public Request(String node_address, String address, String secretKey){
        this.creationTime = System.currentTimeMillis();
        this.origin_address = AES.encrypt(address, secretKey) ;;
        this.status = AES.encrypt("na", secretKey) ;;
        this.response = new ArrayList();
        this.contact_node_address = node_address;
    }

    /**
     * Função que vai buscar o endereço de origem.
     * @param   secretKey    chava para desencriptação
     * @return               endereço de origem
     */
    public String getOrigin_address(String secretKey) {
        return AES.decrypt(this.origin_address, secretKey);
    }

    /**
     * Função que vai buscar o endereço do nó que criou o Request.
     * @param   secretKey    chava para desencriptação
     * @return               endereço do nó que criou o Request
     */
    public String getContactNodeAddress(String secretKey) {
        return AES.decrypt(this.contact_node_address, secretKey);
    }

    /**
     * Função que insere o endereço do nó que criou o Request.
     * @param   address      endereço do nó que criou o Request
     * @param   secretKey    chava para desencriptação
     */
    public void setContactNodeAddress(String address, String secretKey) {
        this.contact_node_address = AES.encrypt(address, secretKey);
    }

    /**
     * Função que vai buscar a mensagem contida no Request.
     * @param   secretKey    chava para desencriptação
     * @return               mensagem do Request
     */
    public String getMessage(String secretKey) {
        return AES.decrypt(message, secretKey);
    }

    /**
     * Função que vai buscar a data de criação do Request.
     * @return               data de criação do Request
     */
    public long getCreationTime(){
        return this.creationTime;
    }

    /**
     * Função que insere a mensagem no Request.
     * @param   message      mensagem para transmitir
     * @param   secretKey    chava para desencriptação
     */
    public synchronized void setMessage(String message, String secretKey) {
        this.message = AES.encrypt(message, secretKey);
    }

    /**
     * Função que adiciona linhas à resposta do Request.
     * @param   message      linha para adicionar À resposta
     * @param   secretKey    chava para encriptação
     */
    public void concatenateResponse(String message, String secretKey){
        String s = message + "\r\n";
        this.response.add(AES.encrypt(s, secretKey));
    }

    /**
     * Função que vai buscar a resposta contida no Request.
     * @param   secretKey    chave para desencriptação
     * @return               resposta do Request
     */
    public Object[] getResponse(String secretKey){
        List<String> decrypted = new ArrayList<>();

        for (String s : this.response)
            decrypted.add(AES.decrypt(s, secretKey));

        return decrypted.toArray();
    }

    /**
     * Função que vai buscar o estado do Request.
     * @param   secretKey    chava para desencriptação
     * @return               estado do Request
     */
    public synchronized String getStatus(String secretKey) {
        return AES.decrypt(this.status, secretKey);
    }

    /**
     * Função que insere o estado no Request.
     * @param   status       estado do Request
     * @param   secretKey    chava para desencriptação
     */
    public synchronized void setStatus(String status, String secretKey) {
        this.status = AES.encrypt(status, secretKey);
    }

    /**
     * Comparador para a classe Request.
     * @param    o            objeto a comparar
     * @return                valor booleano da comparação
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return Objects.equals(origin_address, request.origin_address) &&
                Objects.equals(message, request.message);
    }

    /**
     * Função que imprime o Request.
     * @param   secretKey    chava para desencriptação
     */
    public void printRequest(String secretKey){
        System.out.println("Request: ");
        System.out.println("Source: " + AES.decrypt(this.origin_address, secretKey));
        System.out.println("Source: " + this.origin_address);
        System.out.println("Message: " + AES.decrypt(this.message, secretKey));
        System.out.println("Creation Time: " + this.creationTime);
        System.out.println("Status: " + AES.decrypt(this.status, secretKey));
    }
}
