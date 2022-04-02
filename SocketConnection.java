import java.io.*;

// SocketConnection is used to consume in and out buffers, process commands and send 'send' requests.
public class SocketConnection {
    Socket clients;
    String nodeId, remoteId;
    BufferedReader in;
    PrintWriter out;
    Boolean initiator;
    Client parent;

    public SocketConnection(Socket clients, String nodeId, Boolean initiator, Client parent) {
        this.clients = clients;
        this.nodeId = nodeId;
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
                remoteId = in.readLine();
                System.out.println("Server ID " + remoteId + " has been received.");
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
                out.println(this.nodeId);
            }

            else if(cmd_in.equals("SEND_CLIENT_ID")){
                out.println(this.nodeId);
            }

            else if(cmd_in.equals("REQ")){
                String ReqClientId = cmd.readLine();
                Integer ReqClientTS = Integer.valueOf(cmd.readLine());
                String FileName = cmd.readLine();
                System.out.println("Received Request from Client: " + ReqClientId + " which had logical clock value of: "+ ReqClientTS);
                System.out.println("Calling Client: " + this.nodeId +"'s request processor");
                parent.processRequest(ReqClientId, ReqClientTS,FileName );
            }

            else if(cmd_in.equals("REP")){
                String RepClientId = cmd.readLine();
                String FileName = cmd.readLine();
                System.out.println("Received Reply from Client: " + RepClientId);
                parent.processReply(RepClientId,FileName);
            }

            else if(cmd_in.equals("READ_FROM_FILE_ACK")){
                String respondingServerId = cmd.readLine();
                String fileNameRead = cmd.readLine();
                String lastMessageClient = cmd.readLine();
                String lastMessageTimeStamp = cmd.readLine();
                parent.fileReadAcknowledgeProcessor(respondingServerId,fileNameRead,new Message(lastMessageClient,lastMessageTimeStamp));

            }

            else if(cmd_in.equals("WRITE_TO_FILE_ACK")){
                System.out.println("Received ACK for write.");
                String fileName = cmd.readLine();
                parent.processWriteAck(fileName);
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
    public synchronized void write(String fileName, Message message){
        System.out.println("Sending write request from Client ID: " + this.nodeId +" to server with SERVER ID: " + this.getremoteId());
        out.println("WRITE_TO_FILE");
        out.println(fileName);
        out.println(message.clientId);
        out.println(message.timeStamp);
    }

    // read is used to read from a file.
    public synchronized void read(String fileName){
        System.out.println("Sending read request from Client ID: " + this.nodeId +" to server with SERVER ID: " + this.getremoteId());
        out.println("READ_FROM_FILE");
        out.println(fileName);
        out.println(this.nodeId);
    }

    // request is used to send request for file to remote client.
    public synchronized void request(Integer logicalTS, String fileName ){
        System.out.println("SENDING REQ FROM CLIENT WITH CLIENT ID: " + this.nodeId +" to remote CLIENT ID: " + this.getremoteId() + " for file: "+ fileName);
        out.println("REQ");
        out.println(this.nodeId);
        out.println(logicalTS);
        out.println(fileName);
    }

    // reply is used to send replies from client to remote.
    public synchronized void reply(String fileName){
        System.out.println("SENDING REP FROM CLIENT" + this.nodeId +" TO remote CLIENT ID" + this.getremoteId() + " for file: "+ fileName);
        out.println("REP");
        out.println(this.nodeId);
        out.println(fileName);
    }

    // sendEnquire is used to send ENQUIRE request to server.
    public synchronized  void sendEnquire(){
        System.out.println("Send Enquire to Server");
        out.println("ENQUIRE");
        out.println(this.nodeId);
    }

    // serverWriteTest is used to send a write test to server.
    public synchronized  void serverWriteTest() {
        out.println("WRITE_TEST");
    }

    // publish is used to publish on the server.
    public synchronized void publish() {
        out.println("P");
    }

    // Getter function for remoteId.
    public String getremoteId() {
        return remoteId;
    }

    // Setter function for remoteId.
    public void setremoteId(String remoteId) {
        this.remoteId = remoteId;
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


