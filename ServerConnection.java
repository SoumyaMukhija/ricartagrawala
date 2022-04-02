import java.io.*;

// ServerConnection processes commands, sends receive requests.
public class ServerConnection {
    Socket clients;
    String nodeId, remoteId;
    BufferedReader in;
    PrintWriter out;
    Server parent;

    // ServerConnection is used for making the connection with the server.
    public ServerConnection(Socket clients, String nodeId, Boolean isServer,Server parent) {
        this.clients = clients;
        this.nodeId = nodeId;
        this.parent = parent;

        try{
            in = new BufferedReader(new InputStreamReader(this.clients.getInputStream()));
            out = new PrintWriter(this.clients.getOutputStream(), true);
        }
        catch (Exception e){}

        try {
            if(!isServer) {
                out.println("SEND_CLIENT_ID");
                System.out.println("Waiting for client ID...");
                remoteId = in.readLine();
                System.out.println("Client ID " + remoteId + " has been received.");
            }
        }
        catch (Exception e){}

        Thread read = new Thread(){
            public void run(){
                while(rx_cmd(in,out) != 0) { }
            }
        };

        read.setDaemon(true); 	// terminate when main ends
        read.start();
    }


    // rx_cmd reads commands and calls corresponding functions for them.
    public int rx_cmd(BufferedReader cmd,PrintWriter out) {
        try {
            String input = cmd.readLine();
            if (input.equals("WRITE_TEST")) {
                System.out.println("This is a write test.");
            }

            else if(input.equals("WRITE_TO_FILE")){
                System.out.println("Writing to file now...");
                String fileName = cmd.readLine();
                String reqClientId = cmd.readLine();
                String reqClientTS = cmd.readLine();
                parent.writeToFile(fileName, new Message(reqClientId,reqClientTS));
            }

            else if(input.equals("READ_FROM_FILE")){
                System.out.println("Reading from file now...");
                String fileName = cmd.readLine();
                String reqClientId = cmd.readLine();
                parent.readLastFile(fileName , reqClientId);
            }

            else if( input.equals("ENQUIRE")){
                String reqClientId = cmd.readLine();
                parent.fileHostedString(reqClientId);
            }

        }
        catch (Exception e){}
        return 1;
    }

    // sendLastMessageOnFile returns the last message that was written on the file.
    public synchronized void sendLastMessageOnFile (String fileName, Message lastMsg){
        out.println("READ_FROM_FILE_ACK");
        out.println(this.nodeId);
        out.println(fileName);
        out.println(lastMsg.getClientId());
        out.println(lastMsg.getTimeStamp());
    }

    // sendWriteAcknowledge sends ACK for writing on files.
    public synchronized void sendWriteAcknowledge(String fileName){
        System.out.println("Sending write ACK for " + fileName);
        out.println("WRITE_TO_FILE_ACK");
        out.println(fileName);
    }

    // sendHostedFiles sends response of ENQUIRE, information about the list of hosted files.
    public synchronized void sendHostedFiles(String hostedFiles){
        System.out.println("Sending hosted file information.");
        out.println("ENQUIRE_ACK");
        out.println(hostedFiles);
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
