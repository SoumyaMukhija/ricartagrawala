public class WriteRequest {
	
	String filename;
	Message message;
	
	public WriteRequest(String filename, Message message) {
		this.filename = filename;
		this.message = message;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setMessage(Message message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
}
