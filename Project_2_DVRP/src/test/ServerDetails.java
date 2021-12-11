package test;

import java.util.HashMap;

class ServerDetails {
	int id;
	String ipAddress;
	int port;
	int noOfPacketsReceived;
	HashMap<Integer, Integer> neighborsIdAndCost;
	int[][] routingTable;

	ServerDetails() {
		this.neighborsIdAndCost = new HashMap<>();
		this.noOfPacketsReceived = 0;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public HashMap<Integer, Integer> getNeighborsIdAndCost() {
		return neighborsIdAndCost;
	}

	public void setNeighborsIdAndCost(HashMap<Integer, Integer> neighborsIdAndCost) {
		this.neighborsIdAndCost = neighborsIdAndCost;
	}

	public int getNoOfPacketsReceived() {
		return noOfPacketsReceived;
	}

	public void setNoOfPacketsReceived(int noOfPacketsReceived) {
		this.noOfPacketsReceived = noOfPacketsReceived;
	}

	public int[][] getRoutingTable() {
		return routingTable;
	}

	public void setRoutingTable(int[][] routingTable) {
		this.routingTable = routingTable;
	}

}