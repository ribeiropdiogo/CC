package Exceptions;

/**
 * Exceção dedicada à indicada de um error de parametros insuficientes
 * por do cliente de um dado metódo.
 */
public class InsufficientParametersException extends Exception {

	/**
	 * Permite a criação desta Exceção com uma mensagem representativa do erro ocorrido. 
	 * Sendo da responsabilidade da aplicação o estabelecimento desta mensagem.
	 *
	 * @param message Mensagem visada na comunicação da exceção.
	 */
    public InsufficientParametersException(String message){
        super(message);
    }
}
