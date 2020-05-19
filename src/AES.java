import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


/**
 * Classe responsavel pelo mecanismo criptografico utilizado ao
 * longo deste projeto, tirando forte partido das bibliotecas
 * do java para este efeito.
 */
public class AES {

    /**
     * Armazena as propriedades da chave numa classe fornecida pelo Java.
     * Esta variavel de instancia tem como objetivo final tirar partido dos
     * metodos de encriptaçao AES encapsulados pela classe utilizada.
     */
    private static SecretKeySpec secretKey;

    /**
     * Byte dump da chave utilizada na encriptação.
     */
    private static byte[] key;

    /**
     * Permite indicar a nova chave de segurança a ser utilizada pelo
     * sistema, de forma a anunciar uma potencial nova encriptação.
     *
     * @param myKey A nova chave a ser considerar no mecanismo.
     */
    public static void setKey(String myKey)
    {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metódo que permite a encriptação de mensagens de acordo com uma dada chave.
     *
     * @param strToEncrypt Mensagem não-encriptada, sobre a qual se pretende aplicar encriptação usando uma chave secreta.
     * @param secret Chave de segurança que se pretende aplicar a mensagem indicada.
     *
     * @return Mensagem encriptada com a chave de segurança indicada.
     */
    public static String encrypt(String strToEncrypt, String secret)
    {
        try
        {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        }
        catch (Exception e)
        {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

	/**
     * Metódo que permite a desincriptação de mensagens de acordo com uma dada chave.
     *
     * @param strToDecrypt Mensagem encriptada, sobre a qual se pretende aplicar decriptação usando uma chave secreta.
     * @param secret Chave de segurança que se pretende aplicar a mensagem indicada.
     *
     * @return Mensagem decriptada com a chave de segurança indicada.
     */
    public static String decrypt(String strToDecrypt, String secret)
    {
        try
        {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        }
        catch (Exception e)
        {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }
}