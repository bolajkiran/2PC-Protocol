import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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
                managePartipantsConnections();
                return;
            }));
            connectionThread_participants.start();
        }


        if (pid == 0) {
            connectionThread_coordinator = (new Thread(() -> {
                manageCoordinateConnection();
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

    public void managePartipantsConnections(){
        int server_port = process_portList[pid];
        try {
            server_socket = new ServerSocket(server_port, 0, server_host_addr);
            while(true){
                Socket clientSocket = server_socket.accept();
                ObjectInputStream obj_ip = new ObjectInputStream(clientSocket.getInputStream());
                Thread.sleep(4000);
                Message receivedMessage = (Message) obj_ip.readObject();
                handleReceivedMessage(receivedMessage.getMessage());
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void manageCoordinateConnection(){
        int server_port = process_portList[0];
        try {
            server_socket = new ServerSocket(server_port, 0, server_host_addr);
            server_socket.setSoTimeout(50000);
            while(true){
                Socket clientSocket = server_socket.accept();
                ObjectInputStream obj_ip = new ObjectInputStream(clientSocket.getInputStream());
                Message receivedMessage = (Message) obj_ip.readObject();
                System.out.println(getTimeStamp() + "  Received - " + receivedMessage.getMessage() + " from " + receivedMessage.getParticipantId());
                if (receivedMessage.getMessage().equals(LogRecord.VOTE_COMMIT)) {
                    synchronized ((Object) voteCommitCounter) {
                        voteCommitCounter++;
                        if (voteCommitCounter == num_processes - 1) {
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //perform actions at Participant side
    private void handleReceivedMessage(String receivedMessage) {
        if (receivedMessage.equals(LogRecord.VOTE_REQUEST)) {
            System.out.println(getTimeStamp() + "  Received - " + LogRecord.VOTE_REQUEST + " from C1");
            if (pid == 1) {
                logRecord.write(LogRecord.VOTE_ABORT);
                sendMessageToCoordinator(LogRecord.VOTE_ABORT, process_portList[0]);
            } else {
                logRecord.write(LogRecord.VOTE_COMMIT);
                sendMessageToCoordinator(LogRecord.VOTE_COMMIT, process_portList[0]);
            }
            // }
        } else {
            if (receivedMessage.equals(LogRecord.GLOBAL_COMMIT)) {
                System.out.println(getTimeStamp() + "  Received - " + LogRecord.GLOBAL_COMMIT + " from C1");
                logRecord.write(LogRecord.GLOBAL_COMMIT);
                System.out.println(getTimeStamp() + "  Committed!");
            } else if (receivedMessage.equals(LogRecord.GLOBAL_ABORT)) {
                System.out.println(getTimeStamp() + "  Received - " + LogRecord.GLOBAL_ABORT + " from C1");
                System.out.println(getTimeStamp() + "  Aborted!");
                logRecord.write(LogRecord.GLOBAL_ABORT);

            } else {
                logRecord.write(LogRecord.VOTE_ABORT);
                sendMessageToCoordinator(LogRecord.VOTE_ABORT, process_portList[0]);
                System.out.println(getTimeStamp() + "  Aborted!");
            }

        }
    }


    public void sendMessageToCoordinator(String message, int port) {
        Socket socket = null;
        try {
            socket = new Socket(ProcessHandler.server_host_addr, port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            System.out.println(getTimeStamp() + "  Sending - " + message + " to C1");
            Message message1 = new Message(message);
            message1.setParticipantId(process_id);
            oos.writeObject(message1);
            socket.close();
            System.out.println(getTimeStamp() + "  Sent " + message + " to C1");
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
            for(int i = 1; i < process_portList.length; i++){
                int port = ProcessHandler.process_portList[i];
                socket = new Socket(ProcessHandler.server_host_addr, port);
                obj_op = new ObjectOutputStream(socket.getOutputStream());
                obj_op.writeObject(new Message(message));

                obj_op.close();
                socket.close();
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
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


