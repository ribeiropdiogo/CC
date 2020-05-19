import java.util.Comparator;

/**
 * A classe RequestComparator serve para implementar um comparador que nos permite ordenar
 * Request's numa estrutura de dados, ordenando-os pela sua respetiva data de criação.
 */
public class RequestComparator implements Comparator<Request> {

    /**
     * Dados dois Request's, esta função compara-os e diz-nos qual a sua ordem correta.
     *
     * @param  request1     o primeiro Request a comparar
     * @param  request2     o segundo Request a comparar
     * @return              o valor -1, 0, ou 1 que corresponde à ordem dos Request's
     */
    public int compare(Request request1, Request request2) {
        // for comparison
        if(request1.getCreationTime()==request2.getCreationTime()) {
            return 0;
        } else if(request1.getCreationTime()>request2.getCreationTime()) {
            return 1;
        } else {
            return -1;
        }
    }
}