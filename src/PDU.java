public class PDU {

    private String identifier;
    private int position, control, total_fragments, last;
    private byte[] data;

    public PDU(){

    }

    public String getIdentifier(String secretKey) {
        return AES.decrypt(identifier, secretKey);
    }

    public void setIdentifier(String node, String secretKey) {
        this.identifier = AES.encrypt(node, secretKey);
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getControl() {
        return this.control;
    }

    public void setTotal_fragments(int control) {
        this.total_fragments = control;
    }

    public int getTotal_fragments() {
        return this.total_fragments;
    }

    public void setControl(int control) {
        this.control = control;
    }

    public int getLast() {
        return this.last;
    }

    public void setLast(int last) {
        this.last = last;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}