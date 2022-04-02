public class Message {
	
	String clientID, timestamp;
	
	public Message(String clientID, String timestamp) {
		this.clientID = clientID;
		this.timestamp = timestamp;
	}
	
	public void setClientID(String clientID) {
		this.clientID = clientID;
	}
	
	public String getClientID() {
		return clientID;
	}
	
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
}
