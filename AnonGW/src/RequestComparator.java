import java.util.Comparator;

public class RequestComparator implements Comparator<Request> {

    public int compare(Request request1, Request request2) {

        // for comparison
        if(request1.getCreationTime()==request2.getCreationTime())
            return 0;
        else if(request1.getCreationTime()>request2.getCreationTime())
            return 1;
        else
            return -1;
    }
}