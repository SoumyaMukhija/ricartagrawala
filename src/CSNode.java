public class CSNode {

	String id, ip, portNum, machInfo;
	
	public CSNode(String id, String ip, String portNum, String machInfo) {
		this.id = id;
		this.ip = ip;
		this.portNum = portNum;
	}
	
	public void setID(String id) {
		this.id = id;
	}
	
	public String getID() {
		return id;
	}
	
	public void setIP(String ip) {
		this.ip = ip;
	}
	
	public String getIP() {
		return ip;
	}

	public void setPortNum(String portNum) {
		this.portNum = portNum;
	}
	
	public String getPortNum() {
		return portNum;
	}
	
	public void setMachineInfo(String machInfo) {
		this.machInfo = machInfo;
	}
	
	public String getMachineInfo() {
		return machInfo;
	}
	
}
