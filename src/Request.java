import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Request {
    private long creationTime;
    private String origin_address;
    private String message;
    private List<String> response;
    private String status; // na -> não atendido / ad -> atendido no destino / sd -> servido no destino / to -> a ser transmitido à origem / so -> servido na origem / tbd -> to be deleted

    public Request(String address){
        this.creationTime = System.currentTimeMillis();
        this.origin_address = address;
        this.status = "na";
        this.response = new ArrayList();
    }

    public String getOrigin_address() {
        return origin_address;
    }

    public String getMessage() {
        return message;
    }

    public long getCreationTime(){
        return this.creationTime;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void concatenateResponse(String message){
        String s = message + "\r\n";
        this.response.add(s);
    }

    public Object[] getResponse(){
        return this.response.toArray();
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return Objects.equals(origin_address, request.origin_address) &&
                Objects.equals(message, request.message);
    }

    public void printRequest(){
        System.out.println("Request: ");
        System.out.println("Source: " + this.origin_address);
        System.out.println("Message: " + this.message);
        System.out.println("Creation Time: " + this.creationTime);
        System.out.println("Status: " + this.status);
    }
}
