import java.util.Comparator;

public class PDUComparator implements Comparator<PDU> {

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