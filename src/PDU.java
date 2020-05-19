import java.io.Serializable;

/**
 * A classe PDU é aquela que representa um Protocol Data Unit dentro do nosso sistema. Qualquer
 * Request é encapsulado num PDU que garante a sua distribuição. Também contém mecanismos que nos
 * permitem efetuar retransmissões bem como montar diversos fragmentos.
 */
public class PDU implements Serializable {

    /**
     * Contém um identificador único para cada request encapsulado em PDU's.
     */
    private String identifier;
    /**
     * Contém a posição do fragmento, um inteiro de controlo, o total de fragmentos e o tamanho do Request.
     */
    private int position, control, total_fragments, totalSize;
    /**
     * Contém o payload do PDU.
     */
    private byte[] data;
    /**
     * Timestamp relativo à data de criação do PDU.
     */
    private long timestamp;


    /**
     * Contrutor vazio do PDU.
     */
    public PDU(){
    }

    /**
     * Função que vai buscar o identificador.
     * @param   secretKey    chava para desencriptação
     * @return               identificador do PDU
     */
    public String getIdentifier(String secretKey) {
        return AES.decrypt(identifier, secretKey);
    }

    /**
     * Função que insere o identificador.
     * @param   node       identificador
     * @param   secretKey  chave para encriptação
     */
    public void setIdentifier(String node, String secretKey) {
        this.identifier = AES.encrypt(node, secretKey);
    }

    /**
     * Função que vai buscar a posição.
     * @return               posição do PDU
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * Função que insere a posição.
     * @param   position       posição
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Função que vai buscar o int de cotrolo.
     * @return               int de controlo do PDU
     */
    public int getControl() {
        return this.control;
    }

    /**
     * Função que insere o total de fragmentos do PDU.
     * @param   control    itotal de fragmentos do PDU
     */
    public void setTotal_fragments(int control) {
        this.total_fragments = control;
    }

    /**
     * Função que vai buscar o total de fragmentos.
     * @return               total de fragmentos do PDU
     */
    public int getTotal_fragments() {
        return this.total_fragments;
    }

    /**
     * Função que insere o int de controlo do PDU
     * @param   control    int de controlo do PDU
     */
    public void setControl(int control) {
        this.control = control;
    }

    /**
     * Função que vai buscar o tamanho total do Request.
     * @return               tamanho total do Request
     */
    public int getTotalSize() {
        return this.totalSize;
    }

    /**
     * Função que insere o tamanho total do Request.
     * @param   totalSize    tamanho total do Request
     */
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    /**
     * Função que vai buscar os dados contidos no PDU.
     * @return               dados contidos no PDU
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * Função que insere os dados contidos no PDU.
     * @param   data    dados contidos no PDU.
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Função que vai buscar o timestamp do PDU.
     * @return               timestamp do PDU
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Função que insere o timestamp do PDU.
     * @param   timestamp    timestamp do PDU.
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
