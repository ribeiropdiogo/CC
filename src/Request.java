import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Request implements Serializable {

    private long creationTime;
    private String origin_address;
    private String contact_node_address;
    private String message;
    private List<String> response;
    private String status; // na -> não atendido / ad -> atendido no destino / sd -> servido no destino / to -> a ser transmitido à origem / so -> servido na origem / tbd -> to be deleted

    public Request(String node_address, String address, String secretKey){
        this.creationTime = System.currentTimeMillis();
        this.origin_address = AES.encrypt(address, secretKey) ;;
        this.status = AES.encrypt("na", secretKey) ;;
        this.response = new ArrayList();
        this.contact_node_address = node_address;
    }

    public String getOrigin_address(String secretKey) {
        return AES.decrypt(this.origin_address, secretKey);
    }

    public String getContactNodeAddress(String secretKey) {
        return AES.decrypt(this.contact_node_address, secretKey);
    }

    public void setContactNodeAddress(String address, String secretKey) {
        this.contact_node_address = AES.encrypt(address, secretKey);
    }

    public String getMessage(String secretKey) {
        return AES.decrypt(message, secretKey);
    }

    public long getCreationTime(){
        return this.creationTime;
    }

    public synchronized void setMessage(String message, String secretKey) {
        this.message = AES.encrypt(message, secretKey);
    }

    public void concatenateResponse(String message, String secretKey){
        String s = message + "\r\n";
        this.response.add(AES.encrypt(s, secretKey));
    }

    public Object[] getResponse(String secretKey){
        List<String> decrypted = new ArrayList<>();

        for (String s : this.response)
            decrypted.add(AES.decrypt(s, secretKey));

        return decrypted.toArray();
    }

    public synchronized String getStatus(String secretKey) {
        return AES.decrypt(this.status, secretKey);
    }

    public synchronized void setStatus(String status, String secretKey) {
        this.status = AES.encrypt(status, secretKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return Objects.equals(origin_address, request.origin_address) &&
                Objects.equals(message, request.message);
    }

    public void printRequest(String secretKey){
        System.out.println("Request: ");
        System.out.println("Source: " + AES.decrypt(this.origin_address, secretKey));
        System.out.println("Source: " + this.origin_address);
        System.out.println("Message: " + AES.decrypt(this.message, secretKey));
        System.out.println("Creation Time: " + this.creationTime);
        System.out.println("Status: " + AES.decrypt(this.status, secretKey));
    }
}
