import java.io.*;
import java.net.*;

// SocketConnection is used to consume in and out buffers, process commands and send 'send' requests.
public class SocketConnection {
    Socket clients;
    String nodeID, remoteID;
    BufferedReader in;
    PrintWriter out;
    Boolean initiator;
    Client parent;

    public SocketConnection(Socket clients, String nodeID, Boolean initiator, Client parent) {
        this.clients = clients;
        this.nodeID = nodeID;
        this.parent = parent;

        try{
           in = new BufferedReader(new InputStreamReader(this.clients.getInputStream()));
           out = new PrintWriter(this.clients.getOutputStream(), true);
        }
        catch (Exception e){

        }

        try {
            if(!initiator) {
                out.println("SEND_ID");
                System.out.println("Waiting for sender's ID...");
                remoteID = in.readLine();
                System.out.println("Server ID " + remoteID + " has been received.");
            }
        }

        catch (Exception e){

        }
        Thread read = new Thread(){
            public void run(){
                while(rx_cmd(in,out) != 0) { }
            }
        };
        read.setDaemon(true); 	
        read.start();
    }

    // rx_cmd is used to direct incoming requests/commands to the corresponding functions.
    public int rx_cmd(BufferedReader cmd,PrintWriter out) {
        try {
            String cmd_in = cmd.readLine();
            if(cmd_in.equals("P")){
                System.out.println("Publish request recieved from sender");
            }

            else if(cmd_in.equals("SEND_ID")){
                out.println(this.nodeID);
            }

            else if(cmd_in.equals("SEND_CLIENT_ID")){
                out.println(this.nodeID);
            }

            else if(cmd_in.equals("REQ")){
                String ReqClientID = cmd.readLine();
                Integer ReqClientTS = Integer.valueOf(cmd.readLine());
                String Filename = cmd.readLine();
                System.out.println("Received Request from Client: " + ReqClientID + " which had logical clock value of: "+ ReqClientTS);
                System.out.println("Calling Client: " + this.nodeID +"'s request processor");
                parent.processRequest(ReqClientID, ReqClientTS,Filename );
            }

            else if(cmd_in.equals("REP")){
                String RepClientID = cmd.readLine();
                String Filename = cmd.readLine();
                System.out.println("Received Reply from Client: " + RepClientID);
                parent.processReply(RepClientID,Filename);
            }

            else if(cmd_in.equals("READ_FROM_FILE_ACK")){
                String respondingServerID = cmd.readLine();
                String filenameRead = cmd.readLine();
                String lastMessageClient = cmd.readLine();
                String lastMessageTimestamp = cmd.readLine();
                parent.fileReadAcknowledgeProcessor(respondingServerID,filenameRead,new Message(lastMessageClient,lastMessageTimestamp));

            }

            else if(cmd_in.equals("WRITE_TO_FILE_ACK")){
                System.out.println("Received ACK for write.");
                String filename = cmd.readLine();
                parent.processWriteAck(filename);
            }


            else if( cmd_in.equals("ENQUIRE_ACK")){
                System.out.println("Enquire acknowledge received.");
                String allFiles = cmd.readLine();
                parent.setHostedFiles(allFiles);
            }
        }
        catch (Exception e){}
        return 1;
    }

    // write function writes to a file.
    public synchronized void write(String filename, Message msg){
        System.out.println("Sending write request from Client ID: " + this.nodeID +" to server with SERVER ID: " + this.getremoteID());
        out.println("WRITE_TO_FILE");
        out.println(filename);
        out.println(msg.clientID);
        out.println(msg.timestamp);
    }

    // read is used to read from a file.
    public synchronized void read(String filename){
        System.out.println("Sending read request from Client ID: " + this.nodeID +" to server with SERVER ID: " + this.getremoteID());
        out.println("READ_FROM_FILE");
        out.println(filename);
        out.println(this.nodeID);
    }

    // request is used to send request for file to remote client.
    public synchronized void request(Integer logicalTS, String filename ){
        System.out.println("SENDING REQ FROM CLIENT WITH CLIENT ID: " + this.nodeID +" to remote CLIENT ID: " + this.getremoteID() + " for file: "+ filename);
        out.println("REQ");
        out.println(this.nodeID);
        out.println(logicalTS);
        out.println(filename);
    }

    // reply is used to send replies from client to remote.
    public synchronized void reply(String filename){
        System.out.println("SENDING REP FROM CLIENT" + this.nodeID +" TO remote CLIENT ID" + this.getremoteID() + " for file: "+ filename);
        out.println("REP");
        out.println(this.nodeID);
        out.println(filename);
    }

    // sendEnquire is used to send ENQUIRE request to server.
    public synchronized  void sendEnquire(){
        System.out.println("Send Enquire to Server");
        out.println("ENQUIRE");
        out.println(this.nodeID);
    }

    // serverWriteTest is used to send a write test to server.
    public synchronized  void serverWriteTest() {
        out.println("WRITE_TEST");
    }

    // publish is used to publish on the server.
    public synchronized void publish() {
        out.println("P");
    }

    // Getter function for remoteID.
    public String getremoteID() {
        return remoteID;
    }

    // Setter function for remoteID.
    public void setremoteID(String remoteID) {
        this.remoteID = remoteID;
    }

    // getOtherClient returns all other clients.
    public Socket getOtherClient() {
        return clients;
    }

    // setOtherClient sets list of the other clients.
    public void setOtherClient(Socket clients) {
        this.clients = clients;
    }
}

