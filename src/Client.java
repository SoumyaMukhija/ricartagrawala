import java.io.*; //BufferedReader, FileReader, IOException
import java.net.*; //InetAddress, ServerSocket, Socket
//import java.util.concurrent.*; //Executors, ScheduledExecutorService, TimeUnit
import java.util.regex.*; //Matcher, Pattern
import java.util.*;

public class Client {

    Boolean critSectionRWComplete = true, requestCS = true, useCS = true;
    Integer logicClk = 0, minDelay = 5000, maxLogicClkVal = 0, numServer = 0, pendingReplyCnt = 0, wrtAckCnt = 0;
    String id, availFiles = "", fileProcessOpt = "RW", reqCS;
    HashMap<String, Boolean> clientAuthRequired = new HashMap<>();
    HashMap<String, SocketConnection> socketConnectHM = new HashMap<>();
    HashMap<String, SocketConnection> socketConnectHMServer = new HashMap<>();
    List<CSNode> clientNodes = new LinkedList<>();
    List<CSNode> serverNodes = new LinkedList<>();
    List<String> deferredReplies = new LinkedList<>();
    List<SocketConnection> socketConnectList = new LinkedList<>();
    List<SocketConnection> socketConnectListServer = new LinkedList<>();
    ServerSocket serverConnect;

    public Client(String id) {
        this.id = id;
    }

    public Integer getLogicClk() {
        return this.logicClk;
    }

    public void setLogicClk(Integer logicClk) {
        this.logicClk = logicClk;
    }

    public String getID() {
        return this.id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public List<CSNode> getClientNodes() {
        return this.clientNodes;
    }

    public void setClientNodes(List<CSNode> clientNodes) {
        this.clientNodes = clientNodes;
    }

    public List<CSNode> getServerNodes() {
        return this.serverNodes;
    }

    public void setServerNodes(List<CSNode> serverNodes) {
        this.serverNodes = serverNodes;
    }

    //Once client is operating, the CmdParser will check for input from the terminal.
    public class CmdParser extends Thread {

        Client currentClient;

        public CmdParser(Client currentClient) {
            this.currentClient = currentClient;
        }

        Pattern SETUP = Pattern.compile("^SETUP$");
        Pattern SERVER_SETUP = Pattern.compile("^SERVER_SETUP$");
        Pattern SERVER_SETUP_TEST = Pattern.compile("^SERVER_SETUP_TEST$");
        Pattern START = Pattern.compile("^START$");
        Pattern CONNECTION_DETAIL = Pattern.compile("^CONNECTION_DETAIL$");
        Pattern REQUEST = Pattern.compile("^REQUEST$");
        Pattern AUTO_REQUEST = Pattern.compile("^AUTO_REQUEST$");
        Pattern SHOW_FILES = Pattern.compile("^SHOW_FILES$");
        int rx_cmd(Scanner cmd){
            String cmd_in = null;
            if (cmd.hasNext())
                cmd_in = cmd.nextLine();
            Matcher m_SETUP = SETUP.matcher(cmd_in);
            Matcher m_START = START.matcher(cmd_in);
            Matcher m_CONNECTION_DETAIL = CONNECTION_DETAIL.matcher(cmd_in);
            Matcher m_REQUEST = REQUEST.matcher(cmd_in);
            Matcher m_AUTO_REQUEST = AUTO_REQUEST.matcher(cmd_in);
            Matcher m_SERVER_SETUP = SERVER_SETUP.matcher(cmd_in);
            Matcher m_SERVER_SETUP_TEST = SERVER_SETUP_TEST.matcher(cmd_in);
            Matcher m_SHOW_FILES = SHOW_FILES.matcher(cmd_in);
            if(m_SETUP.find()){
                setupConnections(currentClient);
            }

            else if(m_START.find()){
                System.out.println("Socket connection test function");
                sendP();

            }

            else if(m_REQUEST.find()){
                System.out.println("Initiating REQUEST for file :A: critical section");
                sendRequest("a");
            }

            else if(m_CONNECTION_DETAIL.find()){
                System.out.println("Number of socket connection");
                System.out.println(socketConnectList.size());
                Integer i = 0;
                for(i = 0; i < socketConnectList.size(); i++){
                    System.out.println("IP: " + socketConnectList.get(i).getOtherClient().getInetAddress() + "Port: " + socketConnectList.get(i).getOtherClient().getPort() + "ID: " + socketConnectList.get(i).getremoteID());
                }

                for (String key: socketConnectHM.keySet()){
                    System.out.println("ClientID: " + key + "Socket: " + socketConnectHM.get(key).getOtherClient().getPort());
                }
            }

            else if(m_AUTO_REQUEST.find()){
                sendAutoRequest();
            }

            else if (m_SERVER_SETUP.find()){
                setupServerConnection(currentClient);
                enquireHostedFiles();
            }

            else if(m_SERVER_SETUP_TEST.find()){
                sendTestWrite();
            }

            else if( m_SHOW_FILES.find()){
                System.out.println("Hosted Files: "+ availFiles);
            }
            return 1;
        }

        public void run() {
            System.out.println("Enter commands to set-up MESH Connection : START");
            Scanner input = new Scanner(System.in);
            while(rx_cmd(input) != 0) { }
        }
    }

    //To implement and support a server-side test command
    public void sendTestWrite() {
        for (int remoteServer = 0; remoteServer < this.socketConnectListServer.size(); remoteServer++)
            socketConnectListServer.get(remoteServer).serverWriteTest();
    }

    //if clientID==0 this method connects with other clients from 1...n
    public void setupConnections(Client currentClient){
        try {
            System.out.println("CONNECTING CLIENTS");
            Integer clientID;
            for(clientID = Integer.valueOf(this.id) + 1; clientID < clientNodes.size(); clientID ++ ) {
                Socket clientConnection = new Socket(this.clientNodes.get(clientID).getIP(), Integer.valueOf(clientNodes.get(clientID).getPortNum()));
                SocketConnection socketConnection = new SocketConnection(clientConnection, this.getID(), true,currentClient);
                if(socketConnection.getremoteID() == null){
                    socketConnection.setremoteID(Integer.toString(clientID));
                }
                socketConnectList.add(socketConnection);
                socketConnectHM.put(socketConnection.getremoteID(),socketConnection);
                clientAuthRequired.put(socketConnection.getremoteID(),true);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    //Assists in the establishment of a socket connection for all the accessible servers
    public void setupServerConnection(Client currentClient) {
        try{
            System.out.println("CONNECTING SERVER");
            Integer serverID;
            for (serverID =0; serverID < serverNodes.size(); serverID ++){
                Socket serverConnection = new Socket(this.serverNodes.get(serverID).getIP(), Integer.valueOf(this.serverNodes.get(serverID).getPortNum()));
                SocketConnection socketConnectionServer = new SocketConnection(serverConnection,this.getID(),true,currentClient);
                if(socketConnectionServer.getremoteID() == null){
                    socketConnectionServer.setremoteID(Integer.toString(serverID));
                }
                socketConnectListServer.add(socketConnectionServer);
                socketConnectHMServer.put(socketConnectionServer.getremoteID(),socketConnectionServer);
            }

            this.numServer = socketConnectListServer.size();
        }
        catch (Exception e){
            System.out.println("Setup Server Connection Failure");
        }
    }

    //Test Function
    public void sendP(){
        System.out.println("Sending P");
        Integer i;
        for (i=0; i < this.socketConnectList.size(); i++){
            socketConnectList.get(i).publish();
        }
    }

    //Processing read acknowledgement - critical section acknowledgement and delivering pending requests
    public synchronized void fileReadAcknowledgeProcessor(String respondingServerID, String fileNameRead, Message lastMsg){
        System.out.println("Processing read from file request acknowledge");
        System.out.println("CRITICAL SECTION READ - COMPLETED");
        System.out.println("LAST MESSAGE ON FILE " + fileNameRead + " HAD CLIENT ID: " +lastMsg.getClientID() +" AND TIMESTAMP: " + lastMsg.getTimestamp());
        this.critSectionRWComplete = true;
        releaseCSCleanUp();
    }

    //Processing read acknowledgement - Waiting for n servers to respond with acknowledgement before sending a deferred message
    public synchronized void processWriteAck(String filename){
        System.out.println("Inside WRITE_TO_FILE_ACK processor ");
        if(filename.equals(this.reqCS)){
            this.wrtAckCnt = this.wrtAckCnt -1;
            System.out.println(this.wrtAckCnt);
            if(this.wrtAckCnt == 0 ){
                this.critSectionRWComplete = true;
                System.out.println("WRITE TO FILE COMPLETE");
                releaseCSCleanUp();
            }
        }
    }

    //Thread that generates random read/write critical section accesses on random files
    public void sendAutoRequest() {
        Thread sendAuto = new Thread(){
            public void run(){
                try {
                    while(true) {
                        System.out.println("Auto - Generating request");
                        Random r = new Random();
                        char file = availFiles.charAt(r.nextInt(availFiles.length()));
                        String filename = file +".txt";
                        sendRequest(filename);
                        double randFraction = Math.random() * 1000;
                        Integer delay = (int) Math.floor(randFraction) + minDelay;
                        System.out.println("The AUTO REQUEST THREAD thread will sleep for " + delay +" seconds");
                        Thread.sleep(delay);
                    }
                }
                catch (Exception e){}
            }
        };
        sendAuto.setDaemon(true); 	// terminate at end of main
        sendAuto.start();
    }

    //RX - Process incoming requests and defer if required
    public synchronized void processRequest(String RequestingClientID, Integer RequestingClientLogicalClock, String filename) {
        if( filename.equals(this.reqCS)) {
            System.out.println("Inside Process Request for request Client: " + RequestingClientID + " which had logical clock value of: " + RequestingClientLogicalClock);
            this.maxLogicClkVal = Math.max(this.maxLogicClkVal, RequestingClientLogicalClock);
            if (this.useCS || this.requestCS) {
                if (RequestingClientLogicalClock > this.logicClk) {
                    System.out.println("USING OR REQUESTED CS");
                    System.out.println("Highest Logical Clock Value: " + this.maxLogicClkVal);
                    System.out.println("Current Logical Clock Value:" + this.logicClk);
                    System.out.println("************** SHOULD DEFER *********** CONDITION 1 *****************");
                } 
                else if (RequestingClientLogicalClock == this.logicClk) {
                    System.out.println("USING OR REQUESTED CS");
                    System.out.println("Highest Logical Clock Value: " + this.maxLogicClkVal);
                    System.out.println("Current Logical Clock Value:" + this.logicClk);
                    System.out.println("************** SHOULD DEFER *********** CONDITION 2 *****************");
                }

            }
            if (((this.useCS || this.requestCS) && (RequestingClientLogicalClock > this.logicClk)) || ((this.useCS || this.requestCS) && RequestingClientLogicalClock == this.logicClk && Integer.valueOf(RequestingClientID) > Integer.valueOf(this.getID()))) {
                System.out.println("_____________________________________________________________________________________________________");
                System.out.println("Deferred Reply for request Client: " + RequestingClientID + " which had logical clock value of: " + RequestingClientLogicalClock);
                System.out.println("Critical Section Access from this node had CLIENT ID" + this.getID() + "and last updated logical clock is: " + this.logicClk);
                System.out.println("_____________________________________________________________________________________________________");
                this.clientAuthRequired.replace(RequestingClientID, true);
                this.deferredReplies.add(RequestingClientID);
            } 
            else {
                System.out.println("Initiating SEND REPLY without block as defer condition is not met for the same file " + this.reqCS + filename);
                this.clientAuthRequired.replace(RequestingClientID, true);
                SocketConnection requestingSocketConnection = socketConnectHM.get(RequestingClientID);
                requestingSocketConnection.reply(filename);
            }
        }
        else {
            System.out.println("Inside Process Request for ** DIFFERENT FILE ** request Client: " + RequestingClientID + " which had logical clock value of: " + RequestingClientLogicalClock);
            this.maxLogicClkVal = Math.max(this.maxLogicClkVal, RequestingClientLogicalClock);
            System.out.println("Initiating SEND REPLY without block");
            this.clientAuthRequired.replace(RequestingClientID, true);
            SocketConnection requestingSocketConnection = socketConnectHM.get(RequestingClientID);
            requestingSocketConnection.reply(filename);
        }
    }

    //Respond to critical section replies. Enter if all responses have been received.
    public synchronized void processReply(String ReplyingClientID, String filename){
        if(filename.equals(this.reqCS)) {
            System.out.println("Inside Process Reply for replying Client:  " + ReplyingClientID +" for the file " + filename);
            this.clientAuthRequired.replace(ReplyingClientID, false);
            this.pendingReplyCnt = this.pendingReplyCnt - 1;
            if (this.pendingReplyCnt == 0) {
                enterCriticalSection(filename);
            }
        }
        else {
            System.out.println("Inside Process Reply for replying Client:  " + ReplyingClientID +" for the file " + filename + "### NO ACTION TAKEN");
        }
    }

    //Used to consume the client socket connection available and send out request
    public synchronized void sendRequest(String filename) {
        if(!(this.requestCS || this.useCS)) {
            this.requestCS = true;
            this.reqCS = filename;
            this.logicClk = this.maxLogicClkVal + 1;
            System.out.println("Sending Request with logical clock: " + this.logicClk +" requesting CS access for file " + this.reqCS);
            Integer i;
            for (i = 0; i < this.socketConnectList.size(); i++) {
                if (clientAuthRequired.get(socketConnectList.get(i).getremoteID()) == true) {
                    this.pendingReplyCnt = this.pendingReplyCnt + 1;
                    socketConnectList.get(i).request(this.logicClk, this.reqCS);
                }
            }

            if(this.pendingReplyCnt == 0)
                enterCriticalSection(filename);
        }
        else {
            System.out.println("Currently in CS or already requested for CS");
        }
    }

    //Once critical section access is granted, the client connects to the server and performs read/write
    public void enterCriticalSection(String filename) {
        System.out.println("Entering critical section READ/WRITE TO SERVER");
        this.useCS = true;
        this.requestCS = false;
        this.critSectionRWComplete = false;
        Random r = new Random();
        char readOrWrite = fileProcessOpt.charAt(r.nextInt(fileProcessOpt.length()));
        try {
            System.out.println("================= ENTERING CRITICAL SECTION ===================");
            if(readOrWrite == 'R') {
                System.out.println("CRITICAL SECTION READ OPTION");
                //Choosing random server to read from
                Integer serverNumber = r.nextInt(this.getServerNodes().size());
                String serverID = Integer.toString(serverNumber);
                this.critSectionRWComplete = false;
                socketConnectHMServer.get(serverID).read(filename);
                System.out.println("SERVER; " +serverID + " File: " + filename + " PROCESS OPTION: READ");

            }
            else if( readOrWrite == 'W') {
                System.out.println("CRITICAL SECTION WRITE OPTION");
                this.wrtAckCnt = this.numServer;
                Integer serverConnectIndex;

                for (serverConnectIndex = 0; serverConnectIndex < this.socketConnectListServer.size() ; serverConnectIndex ++){
                    this.socketConnectListServer.get(serverConnectIndex).write(filename, new Message(this.getID(), Integer.toString(this.logicClk)));
                }

                System.out.println("SERVER; ALL File: " + filename + " PROCESS OPTION: WRITE");

            }
            System.out.println("========================= EXCITING CRITICAL SECTION ============");
        }
        catch (Exception e) {
            System.out.println("File write error");
        }
    }

    //Following the execution of the critical section, deferred messages will receive responses, and the relevant operations flags will be set
    public void releaseCSCleanUp() {
        System.out.println("Recieved necessary acknowledgement");
        System.out.println("----------ENTERING CLEAN UP: SEND DEFERRED REPLY AND FLAG RESET --------------------------------");
        this.useCS = false;
        this.requestCS = false;
        Iterator<String> deferredReplyClientID = deferredReplies.iterator();
        while(deferredReplyClientID.hasNext()){
            socketConnectHM.get(deferredReplyClientID.next()).reply(this.reqCS);
        }
        this.reqCS = "";
        deferredReplies.clear();
        System.out.println(" ----------------- EXITING CLEAN UP -----------------------------");
    }

    //Used to construct client listener socket, then use the listener to connect to a requesting socket
    public void clientSocket(Integer ClientID, Client currentClient) {
        try {
            serverConnect = new ServerSocket(Integer.valueOf(this.clientNodes.get(ClientID).portNum));
            id = Integer.toString(ClientID);
            System.out.println("Client node running on port " + Integer.valueOf(this.clientNodes.get(ClientID).portNum) +"," + " use ctrl-C to end");
            InetAddress myip = InetAddress.getLocalHost();
            String ip = myip.getHostAddress();
            String hostname = myip.getHostName();
            System.out.println("Your current IP address : " + ip);
            System.out.println("Your current Hostname : " + hostname);
        }
        catch (IOException e) {
            System.out.println("Error creating socket");
            System.exit(-1);
        }
        CmdParser cmdpsr = new CmdParser(currentClient);
        cmdpsr.start();
        Thread current_node = new Thread() {
            public void run() {
                while(true) {
                    try {
                        Socket s = serverConnect.accept();
                        SocketConnection socketConnection = new SocketConnection(s,id,false, currentClient);
                        socketConnectList.add(socketConnection);
                        socketConnectHM.put(socketConnection.getremoteID(),socketConnection);
                        clientAuthRequired.put(socketConnection.getremoteID(),true);
                    }
                    catch(IOException e) {
                        e.printStackTrace(); 
                    }
                }
            }
        };
        current_node.setDaemon(true);
        current_node.start();
    }

    public synchronized void setHostedFiles(String hostedFiles){
        this.availFiles = hostedFiles;
    }

    public void enquireHostedFiles(){
        socketConnectHMServer.get("0").sendEnquire();
    }

    //Using the client configuration file and saving the information
    public void setClientList() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("ClientAddrPorts.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    List<String> parsed_client = Arrays.asList(line.split(","));
                    CSNode n_client= new CSNode(parsed_client.get(0),parsed_client.get(1),parsed_client.get(2),parsed_client.get(3));
                    this.getClientNodes().add(n_client);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String everything = sb.toString();
                System.out.println(everything);
                System.out.println(this.getClientNodes().size());
            } 
            finally {
                br.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Using the server configuration file and saving the information
    public void setServerList() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("ServerAddrPorts.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    List<String> parsed_server = Arrays.asList(line.split(","));
                    CSNode n_server = new CSNode(parsed_server.get(0),parsed_server.get(1),parsed_server.get(2),parsed_server.get(3));
                    this.getServerNodes().add(n_server);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String everything = sb.toString();
                System.out.println(everything);
                System.out.println(this.getServerNodes().size());
            } 
            finally {
                br.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length!=1) {
            System.out.println("Usage: java Client <client-number>");
            System.exit(1);
        }
        System.out.println("Starting the Client");
        Client C1 = new Client(args[0]);
        C1.setClientList();
        C1.setServerList();
        C1.clientSocket(Integer.valueOf(args[0]),C1);
        System.out.println("Started Client with ID: " + C1.getID());
    }

}