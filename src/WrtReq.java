public class WrtReq {
	
	String filename;
	Message msg;
	
	public WrtReq(String filename, Message msg) {
		this.filename = filename;
		this.msg = msg;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setMsg(Message msg) {
		this.msg = msg;
	}
	
	public Message getMsg() {
		return msg;
	}
	
}
