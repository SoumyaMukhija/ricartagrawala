import java.io.*;
import java.net.*;

// ServerConnection processes commands, sends receive requests.
public class ServerConnection {
    Socket clients;
    String nodeID, remoteID;
    BufferedReader in;
    PrintWriter out;
    Server parent;

    // ServerConnection is used for making the connection with the server.
    public ServerConnection(Socket clients, String nodeID, Boolean isServer,Server parent) {
        this.clients = clients;
        this.nodeID = nodeID;
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
                remoteID = in.readLine();
                System.out.println("Client ID " + remoteID + " has been received.");
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
                String filename = cmd.readLine();
                String reqClientID = cmd.readLine();
                String reqClientTS = cmd.readLine();
                parent.writeToFile(filename, new Message(reqClientID, reqClientTS));
            }

            else if(input.equals("READ_FROM_FILE")){
                System.out.println("Reading from file now...");
                String filename = cmd.readLine();
                String reqClientID = cmd.readLine();
                parent.readLastOfFile(filename , reqClientID);
            }

            else if( input.equals("ENQUIRE")){
                String reqClientID = cmd.readLine();
                parent.fileHostedString(reqClientID);
            }

        }
        catch (Exception e){}
        return 1;
    }

    // sendLastMessageOnFile returns the last message that was written on the file.
    public synchronized void sendLastMessageOnFile (String filename, Message lastMsg){
        out.println("READ_FROM_FILE_ACK");
        out.println(this.nodeID);
        out.println(filename);
        out.println(lastMsg.getClientID());
        out.println(lastMsg.getTimestamp());
    }

    // sendWriteAck sends acknowledge for writing on files.
    public synchronized void sendWriteAck(String filename){
        System.out.println("Sending write ACK for " + filename);
        out.println("WRITE_TO_FILE_ACK");
        out.println(filename);
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