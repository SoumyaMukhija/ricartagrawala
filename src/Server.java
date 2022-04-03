import java.io.*; //BufferedReader, FileReader, IOException
import java.net.*; //InetAddress, ServerSocket, Socket
import java.util.regex.*; //Matcher, Pattern
import java.util.*;

public class Server {

    String id, allFiles = "";
    File[] filesList;
    ServerSocket server;
    HashMap<String, ServerConnection> serverConnectHM = new HashMap<>();
    HashMap<String, String> serverFolder = new HashMap<>();
    List<CSNode> serverNodes = new LinkedList<>();
    List<ServerConnection> serverConnectList = new LinkedList<>();

    public Server(String id) {
        this.id = id;
    }

    public String getID() {
        return this.id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public List<CSNode> getServerNodes() {
        return this.serverNodes;
    }

    public void setServerNodes(List<CSNode> serverNodes) {
        this.serverNodes = serverNodes;
    }

    //Parser for terminal operations
    public class Parser extends Thread {

        Server currentServer;

        public Parser(Server currentServer) {
            this.currentServer = currentServer;
        }

        Pattern START = Pattern.compile("^START$");
        Pattern SHOW_FILES = Pattern.compile("^SHOW_FILES$");

        int rx_cmd(Scanner cmd) {
            String cmd_in = null;
            if (cmd.hasNext())
                cmd_in = cmd.nextLine();
            Matcher m_START = START.matcher(cmd_in);
            Matcher m_SHOW_FILES = SHOW_FILES.matcher(cmd_in);

            if(m_START.find()) {
                System.out.println("Socket connection test function");
                try {
                    System.out.println("STATUS UP");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }

            else if(m_SHOW_FILES.find()) {
                currentServer.fileHostedString("0");
            }

            return 1;
        }

        public void run() {
            System.out.println("Enter commands to set-up MESH Connection : START");
            Scanner input = new Scanner(System.in);
            while(rx_cmd(input) != 0) { }
        }
    }

    //Writing to file and sending acknowledgement
    public  synchronized void writeToFile(String filename, Message msg) throws IOException {
        BufferedWriter bfw = new BufferedWriter(new FileWriter( "./"+ this.serverFolder.get(this.getID())+ "/" +filename, true));
        bfw.append(msg.getClientID()+","+msg.getTimestamp()+"\n");
        bfw.close();
        serverConnectHM.get(msg.getClientID()).sendWriteAck(filename);
    }

    //Sending reply for enquire command
    public synchronized void fileHostedString(String requestingClientID){
        if(allFiles.isEmpty()) {
            File folder = new File("./" + this.serverFolder.get(this.getID()) + "/");
            filesList = folder.listFiles();
            for (int fileIndex = 0; fileIndex < filesList.length; fileIndex++)
                this.allFiles = this.allFiles + filesList[fileIndex].getName().substring(0, filesList[fileIndex].getName().lastIndexOf("."));
        }
        serverConnectHM.get(requestingClientID).sendHostedFiles(this.allFiles);
    }

    //Reading final line of file and sending response
    public synchronized void readLastOfFile(String filename, String requestingClientID) {
        String currLine;
        String lastLine= "";
        try {
            BufferedReader bfr = new BufferedReader(new FileReader("./"+ this.serverFolder.get(this.getID())+ "/" +filename));
            try {
                while ((currLine = bfr.readLine()) != null)
                    lastLine = currLine;
            } 
            finally {
                bfr.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Message returnMsg;
        if(!lastLine.isEmpty()) {
            List<String> msg = Arrays.asList(lastLine.split(","));
            System.out.println("Returning last line read as Message");
            returnMsg = new Message(msg.get(0), msg.get(1));
        }
        else {
            returnMsg = new Message("EMPTY FILE - NO CLIENT ID", "EMPTY FILE - NO TIME STAMP");
        }
        serverConnectHM.get(requestingClientID).sendLastMessageOnFile(filename, returnMsg);
    }

    //Consume the configuration file and set work folder for specified server ID
    public void setServerFolder() {
        try {
            BufferedReader bfr = new BufferedReader(new FileReader("ServerFolders.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = bfr.readLine();
                while (line != null) {
                    sb.append(line);
                    List<String> parsed_server_workFolder = Arrays.asList(line.split(","));
                    this.serverFolder.put(parsed_server_workFolder.get(0),parsed_server_workFolder.get(1));
                    sb.append(System.lineSeparator());
                    line = bfr.readLine();
                }
                String everything = sb.toString();
                System.out.println(everything);
                System.out.println(this.getServerNodes().size());
            } 
            finally {
                bfr.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Consume the server configuration file to set the current node's settings
    public void setServerList() {
        try {
            BufferedReader bfr = new BufferedReader(new FileReader("ServerAddrPorts.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = bfr.readLine();
                while (line != null) {
                    sb.append(line);
                    List<String> parsed_server = Arrays.asList(line.split(","));
                    CSNode n_server = new CSNode(parsed_server.get(0),parsed_server.get(1),parsed_server.get(2),parsed_server.get(3));
                    this.getServerNodes().add(n_server);
                    sb.append(System.lineSeparator());
                    line = bfr.readLine();
                }
                String everything = sb.toString();
                System.out.println(everything);
                System.out.println(this.getServerNodes().size());
            } 
            finally {
                bfr.close();
            }
        }
        catch (Exception e) {
        }
    }

    //Used to construct server listener socket, then use the listener to connect to a requesting socket
    public void serverSocket(int serverID, Server currentServer) {
        try {
            server = new ServerSocket(Integer.valueOf(this.serverNodes.get(serverID).portNum));
            id = Integer.toString(serverID);
            System.out.println("Server node running on port " + Integer.valueOf(this.serverNodes.get(serverID).portNum) +"," + " use ctrl-C to end");
            InetAddress myServerIp = InetAddress.getLocalHost();
            String ip = myServerIp.getHostAddress();
            String hostname = myServerIp.getHostName();
            System.out.println("Your current Server IP address : " + ip);
            System.out.println("Your current Server Hostname : " + hostname);
        }
        catch (IOException e) {
            System.out.println("Error creating socket");
            System.exit(-1);
        }
        Server.Parser cmdpsr = new Server.Parser(currentServer);
        cmdpsr.start();
        Thread current_node = new Thread() {
            public void run(){
                while(true){
                    try{
                        Socket s = server.accept();
                        ServerConnection serverConnection = new ServerConnection(s,id, false,currentServer);
                        serverConnectList.add(serverConnection);
                        serverConnectHM.put(serverConnection.getremoteID(), serverConnection);
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

    public static void main(String[] args) {
        if (args.length!=1) {
            System.out.println("Usage: java Server <server-number>");
            System.exit(1);
        }
        System.out.println("Starting the server");
        Server server = new Server(args[0]);
        server.setServerList();
        server.setServerFolder();
        server.serverSocket(Integer.valueOf(args[0]),server);
        System.out.println("Started Server with ID: " + server.getID());
    }
    
}