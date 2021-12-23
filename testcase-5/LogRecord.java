import java.util.concurrent.ConcurrentLinkedDeque;

public class LogRecord {
    public static final String START_2PC = "START_2PC",
            GLOBAL_ABORT = "GLOBAL_ABORT",
            GLOBAL_COMMIT = "GLOBAL_COMMIT",
            VOTE_ABORT = "VOTE_ABORT",
            VOTE_COMMIT = "VOTE_COMMIT",
            VOTE_REQUEST = "VOTE_REQUEST",
            DECISION_REQUEST = "DECISION_REQUEST",
            DECISION_PENDING = "DECISION_PENDING";


    private static ConcurrentLinkedDeque<String> log;

    public void write(String msg){
        log = new ConcurrentLinkedDeque<String>();
        log.add(msg);
        System.out.println(ProcessHandler.getTimeStamp() + "  LOG: " + msg);
    }

    public String latest(){
        return log.peekLast();
    }
}

