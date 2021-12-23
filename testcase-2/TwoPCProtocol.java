import java.io.IOException;

public class TwoPCProtocol {
    public static void main(String[] args) throws InterruptedException, IOException {
        ProcessHandler processHandler = new ProcessHandler(4, args[0]);
        processHandler.start2PC();
    }
}

