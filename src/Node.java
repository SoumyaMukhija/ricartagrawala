public class Node {

	String id, port, ipAddress;
	
	public Node(String id, String port, String ipAddress) {
		this.id = id;
		this.port = port;
		this.ipAddress = ipAddress;
	}
	
	public void setID(String id) {
		this.id = id;
	}
	
	public String getID() {
		return id;
	}
	
	public void setPort(String port) {
		this.port = port;
	}
	
	public String getPort() {
		return port;
	}
	
	public void setIPAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	public String getIPAddress() {
		return ipAddress;
	}

}
