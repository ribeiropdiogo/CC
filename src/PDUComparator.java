import java.util.Comparator;

/**
 * A classe PDUComparator serve para implementar um comparador que nos permite ordenar PDU's numa
 * estrutura de dados, ordenando-os pelo número do fragmento a que correspondem.
 */
public class PDUComparator implements Comparator<PDU> {

    /**
     * Dados dois PDU's, esta função compara-os e diz-nos qual a sua ordem correta.
     *
     * @param  pdu1     o primeiro PDU a comparar
     * @param  pdu2     o segundo PDU a comparar
     * @return          o valor -1, 0, ou 1 que corresponde à ordem dos PDU's
     */
    public int compare(PDU pdu1, PDU pdu2) {
        // for comparison
        if(pdu1.getPosition()==pdu2.getPosition()) {
            return 0;
        } else if(pdu1.getPosition()<pdu2.getPosition()) {
            return -1;
        } else {
            return 1;
        }
    }
}