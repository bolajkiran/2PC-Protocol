import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProcessHandler {

    public static String process_id; //Ex P1
    public static int pid; // Ex. 1

    public static InetAddress server_host_addr = InetAddress.getLoopbackAddress();
    private static ServerSocket server_socket;
    public static int[] process_portList;
    public Thread connectionThread_participants = null;
    public Thread connectionThread_coordinator = null;
    public Thread communicationThread = null;
    public static LogRecord logRecord = new LogRecord();
    public static int num_processes;
    public static volatile int voteCommitCounter = 0;
    public static volatile int p1_failed_flag = 0;
    public static int p1_flag = 1;
    public static int timeOutFlag = 0;
    public static String transactionState = LogRecord.DECISION_REQUEST;

    public ProcessHandler(int num_processes, String processId)  {
        pid = Integer.parseInt(processId);
        if (Integer.parseInt(processId) == 0) {
            String id = String.valueOf(Integer.parseInt(processId) + 1);
            process_id = "C" + id;
        } else {
            process_id = "P" + processId;
        }

        this.num_processes = num_processes;

        process_portList = new int[num_processes];
        for (int i = 0; i < num_processes; i++) {
            process_portList[i] = 2001 + i;
        }
    }

    public void start2PC() throws IOException {
        if (pid == 0) {
            System.out.println("COORDINATOR: " + process_id);
        } else {
            System.out.println("PARTICIPANT: " + process_id);
        }
        System.out.println("\t\t    ___");
        System.out.println("\t\t   / []\\");
        System.out.println("\t\t _|_____|_");
        System.out.println("\t\t| | === | |");
        System.out.println("\t\t|_|  0  |_|");
        System.out.println("\t\t ||_____||");
        System.out.println("\t\t|~ \\___/ ~|");
        System.out.println("\t\t/=\\ /=\\ /=\\");
        System.out.println("\t\t[_] [_] [_]\n");

        //Thread 01: manageConnections
        if (pid != 0) {
            connectionThread_participants = (new Thread(() -> {
                manageParticipantsConnections();
                return;
            }));
            connectionThread_participants.start();
        }


        if (pid == 0) {
            connectionThread_coordinator = (new Thread(() -> {
                try {
                    manageCoordinateConnection();
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                return;
            }));
            connectionThread_coordinator.start();
        }

        try {
            // Added this delay to wait till all other processes are in the listening state
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (pid == 0) {
            performCoordinatorActions();
        }
    }

    public void manageParticipantsConnections(){
        int server_port = process_portList[pid];
        try {
            server_socket = new ServerSocket(server_port, 0, server_host_addr);
            while(true){
                try {
                    Socket clientSocket = server_socket.accept();
                    ObjectInputStream obj_ip = new ObjectInputStream(clientSocket.getInputStream());
                    Thread.sleep(2000);

                    //System.out.println(process_id + ": " + p1_flag);
                    Message receivedMessage = (Message) obj_ip.readObject();
                    handleReceivedMessage(receivedMessage, receivedMessage.getParticipantId());
                    if (p1_flag == 1) {
                        server_socket.setSoTimeout(5000);
                        p1_flag++;
                    }
                    clientSocket.close();
                } catch (SocketTimeoutException e) {
                    if (timeOutFlag == 0) {
                        System.out.println(getTimeStamp() + "  Didn't receive any response from transaction decision from C1...");
                        System.out.println(getTimeStamp() + "  Contacting other participants for the transaction decision...");
                        Message message = new Message(LogRecord.DECISION_REQUEST);
                        int portId = (pid + 1) % 3 == 0 ? (pid - 1) : (pid + 1) % 3;
                        message.setParticipantId("P" + pid);
                        timeOutFlag++;
                       // (new Thread(() -> sendMessageToParticipant(message, process_portList[portId], "P" + portId))).start();
                        sendMessageToParticipant(message, process_portList[portId], "P" + portId);
                    }

                }
            }
        }
        catch (IOException | InterruptedException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void manageCoordinateConnection() throws SocketException {
        int server_port = process_portList[0];

        try {
            server_socket = new ServerSocket(server_port, 0, server_host_addr);
            while(true){
                Socket clientSocket = server_socket.accept();
                ObjectInputStream obj_ip = new ObjectInputStream(clientSocket.getInputStream());
                Message receivedMessage = (Message) obj_ip.readObject();
                System.out.println(getTimeStamp() + "  Received - " + receivedMessage.getMessage() + " from " + receivedMessage.getParticipantId());
                if (receivedMessage.getMessage().equals(LogRecord.VOTE_COMMIT)) {
                    synchronized ((Object) voteCommitCounter) {
                        voteCommitCounter++;
                        if (voteCommitCounter == num_processes - 1) {
                            System.out.println(getTimeStamp() + "  This process (C1) has stopped!");
                            Thread.sleep(15000);
                            System.out.println(getTimeStamp() + "  Recovering from failure...");
                            Thread.sleep(15000);
                            System.out.println(getTimeStamp() + "  Recovered now from failure!");
                            multicastMessageToAllParticipants(LogRecord.GLOBAL_COMMIT);
                            logRecord.write(LogRecord.GLOBAL_COMMIT);
                        }
                    }
                } else if (receivedMessage.getMessage().equals(LogRecord.VOTE_ABORT)) {
                    multicastMessageToAllParticipants(LogRecord.GLOBAL_ABORT);
                    logRecord.write(LogRecord.GLOBAL_ABORT);
                }
                clientSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //perform actions at Participant side
    private void handleReceivedMessage(Message receivedMessage, String senderProcessID) {
        if (receivedMessage.getMessage().equals(LogRecord.VOTE_REQUEST)) {
            System.out.println(getTimeStamp() + "  Received - " + LogRecord.VOTE_REQUEST + " from " + senderProcessID);
            logRecord.write(LogRecord.VOTE_COMMIT);
            Message message = new Message(LogRecord.VOTE_COMMIT);
            message.setParticipantId(process_id);
            sendMessageToCoordinator(message, process_portList[0]);
        } else if (receivedMessage.getMessage().equals(LogRecord.DECISION_REQUEST)) {
            System.out.println(getTimeStamp() + "  Received - " + LogRecord.DECISION_REQUEST + " from " + receivedMessage.getParticipantId());
            Message message = new Message(LogRecord.DECISION_PENDING);
            message.setParticipantId(receivedMessage.getParticipantId());
            sendMessageToParticipant(message, process_portList[Integer.parseInt(senderProcessID.substring(senderProcessID.indexOf("P") + 1))], senderProcessID);
        } else if (receivedMessage.getMessage().equals(LogRecord.DECISION_PENDING)) {
            System.out.println(getTimeStamp() + "  Received - " + LogRecord.DECISION_REQUEST + " from " + receivedMessage.getParticipantId());
            System.out.println(getTimeStamp() + "  Waiting for C1 recovery....");
        }
        else {
            if (receivedMessage.getMessage().equals(LogRecord.GLOBAL_COMMIT)) {
                System.out.println(getTimeStamp() + "  Received - " + LogRecord.GLOBAL_COMMIT + " from " + senderProcessID);
                logRecord.write(LogRecord.GLOBAL_COMMIT);
                System.out.println(getTimeStamp() + "  Committed!");
            } else if (receivedMessage.getMessage().equals(LogRecord.GLOBAL_ABORT)) {
                System.out.println(getTimeStamp() + "  Received - " + LogRecord.GLOBAL_ABORT + " from " + senderProcessID);
                System.out.println(getTimeStamp() + "  Aborted!");
                logRecord.write(LogRecord.GLOBAL_ABORT);

            } else {
                logRecord.write(LogRecord.VOTE_ABORT);
                Message message = new Message(LogRecord.VOTE_ABORT);
                message.setParticipantId("C1");
                sendMessageToCoordinator(message, process_portList[0]);
                System.out.println(getTimeStamp() + "  Aborted!");
            }
        }
    }


    public void sendMessageToParticipant(Message message, int port, String receiverID) {
        Socket socket = null;
        try {
            socket = new Socket(ProcessHandler.server_host_addr, port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            System.out.println(getTimeStamp() + "  Sending - " + message.getMessage() + " to " + receiverID);
            oos.writeObject(message);
            System.out.println(getTimeStamp() + "  Sent " + message.getMessage() + " to " + receiverID);
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToCoordinator(Message message, int port) {
        Socket socket = null;
        try {
            socket = new Socket(ProcessHandler.server_host_addr, port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            System.out.println(getTimeStamp() + "  Sending - " + message.getMessage() + " to C1");
            oos.writeObject(message);
            System.out.println(getTimeStamp() + "  Sent " + message.getMessage() + " to C1");
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void performCoordinatorActions()  {
        System.out.println(getTimeStamp() + "  Starting 2PC");
        logRecord.write(LogRecord.START_2PC);
        multicastMessageToAllParticipants(LogRecord.VOTE_REQUEST);
    }

    public void multicastMessageToAllParticipants(String message) {
        System.out.println(getTimeStamp() + "  Sending " + message + " to all the participants...");
        try {
            Socket socket;
            ObjectOutputStream obj_op;
            // Multicast to all the processes
            for(int i = process_portList.length - 1; i > 0; i--){
                int port = ProcessHandler.process_portList[i];
                socket = new Socket(ProcessHandler.server_host_addr, port);
                obj_op = new ObjectOutputStream(socket.getOutputStream());
                Message message1 = new Message(message);
                message1.setParticipantId("C1");
                obj_op.writeObject(message1);
                obj_op.close();
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getTimeStamp() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
        String formattedDate = sdf.format(date);
        return formattedDate;
    }
}


