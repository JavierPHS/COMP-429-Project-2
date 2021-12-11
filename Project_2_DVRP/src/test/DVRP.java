package test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
//import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;


class Message{
	String operation;
	HashMap<Integer, Integer> link=new HashMap<>();
	int[][] rt;
}



public class DVRP {

	public ArrayList<ServerDetails> serverList =new ArrayList<>();
	int[][] routingTableReadFromTopologyFile;
	int updateInterval=1000;
	int myServerId;
	int myPort;
	HashMap<Integer, Integer> nextHop = new HashMap<>();
	String myIP;
	ServerSocket serverSocket;
	int numDisabledServers = 0;
	int numPackets = 0;
	

	public static void main(String[] args) {

		System.out.println("Beginning Program...");
		DVRP dvrp = new DVRP();
		dvrp.menu();
	}
	
	public void menu() {
		Scanner stdinScanner = new Scanner(System.in);
		while (true) {
			System.out.print(">>");

			String line = stdinScanner.nextLine();
			String[] splitLine = line.split(" ");

			if (splitLine.length < 1) {
				System.out.print("Incorrect Command ");
				continue;
			}

			switch (splitLine[0]) {
			case "server":

				try {
					updateInterval = Integer.parseInt(splitLine[4]);
				} catch (Exception e) {
					System.out.println("Server Command Incorrect");
					continue;
				}
				serverList = readTopologyFile(splitLine[2], serverList);

				serverList = createRoutingTable(serverList);
				
				//Create Timer that will automatically send out routing table update every time a set interval has passed
				updateInterval = updateInterval*1000;
				Timer timer = new Timer();
        		ScheduledTask st = new ScheduledTask();
        		timer.schedule(st, updateInterval, updateInterval);

				routingTableReadFromTopologyFile = new int[serverList.size()+numDisabledServers][serverList.size()+numDisabledServers];
				for(int i=0;i<serverList.size();i++) {
					if(serverList.get(i).id == myServerId) {
						for(int s=0;s<serverList.get(i).routingTable.length;s++) {
							for(int t=0;t<serverList.get(i).routingTable[s].length;t++) {
								routingTableReadFromTopologyFile[s][t] = serverList.get(i).routingTable[s][t];
							}
						}
						break;
					}
				}

				System.out.println(splitLine[0] + " SUCCESS\n");
				break;
			case "help":
				System.out.println(line + " SUCCESS");
				System.out.println("\nList of Commands supported:" + "\n>> help"
						+ "\n>> update <server id 1> <server id 2> <link cost>" + "\n>> step" + "\n>> packets"
						+ "\n>> display" + "\n>> disable <server id>" + "\n>> crash\n");
				break;
			case "update":
				int linkServer1 = Integer.parseInt(splitLine[1]);
				int linkServer2 = Integer.parseInt(splitLine[2]);
				String newCostOfLink =  splitLine[3];
				if(linkServer1 == linkServer2)
				{
					System.out.println("Enter command correctly");
					break;
				}
				else if(linkServer2 == myServerId)
				{
					sendUpdateLinkCostToNeighbor(linkServer2,linkServer1,newCostOfLink);
					break;
				}
				else
				{
					sendUpdateLinkCostToNeighbor(linkServer1,linkServer2,newCostOfLink);
					break;
				}
			case "step":
				doStep(serverList);
				System.out.println("STEP SUCCESS\n");
				break;
			case "packets":
				displayPackets(serverList);
				System.out.println("PACKETS SUCCESS\n");
				break;
			case "display":
				System.out.println();
				displayRoutingTable(serverList);
				System.out.println("\nDISPLAY SUCCESS\n");
				break;
			case "disable":
				//send this to all servers, not just neighbors
				if(Integer.parseInt(splitLine[1])==(myServerId))
				{
					System.out.println("You cannot disable yourself");
					break;
				}
				sendDisableToserverList(Integer.parseInt(splitLine[1]));
				numDisabledServers++;
				System.out.println("DISABLE SUCCESS");
				break;
			case "crash":
				sendCrashToserverList();
				System.out.println("SERVER CRASH. SHUTTING DOWN...");
				System.exit(1);
				break;
			default:
				break;
			}

		}
	}


	private void sendUpdateLinkCostToNeighbor(int linkServer1, int linkServer2, String newCostOfLink) {

		if(newCostOfLink.equalsIgnoreCase("inf")) {
			routingTableReadFromTopologyFile[linkServer1-1][linkServer2-1] = 9999;
		}
		else {
			routingTableReadFromTopologyFile[linkServer1-1][linkServer2-1] = Integer.parseInt(newCostOfLink);
		}


		for(int x=0;x<serverList.size();x++) {
			if(serverList.get(x).id == myServerId) {

				for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
					for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
						serverList.get(x).routingTable[i][j] = routingTableReadFromTopologyFile[i][j];
					}
				}
				break;
			}
		}

		JSONObject obj=new JSONObject();
		try {
			obj.put("operation", "update");
			obj.put("update_server_id_1", linkServer1);
			obj.put("update_server_id_2", linkServer2);
			obj.put("cost", newCostOfLink);
			obj.put("sender_id", myServerId);

		}
		catch(Exception e) {
			System.out.println("JSON Object Error");
			e.getStackTrace();
		}
		try {
			for(int i=0;i<serverList.size();i++) {
				InetAddress ip=InetAddress.getByName(serverList.get(i).ipAddress);
				Socket socket = new Socket(ip, serverList.get(i).port);
				//System.out.println("socket Created");
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

				// transfer JSONObject as String to the server
				dataOutputStream.writeUTF(obj.toString());
				//socket.close();

			}
		} catch (Exception e) {
			System.out.println("Connection(s) failed");
		}
		updateRoutingTable(serverList, routingTableReadFromTopologyFile);
		doStep(serverList);
	}


	private void sendCrashToserverList() {
		JSONObject obj=new JSONObject();
		try {
			obj.put("operation", "crash");
			obj.put("server_id", myServerId);
		}
		catch(Exception e) {
			System.out.println("JSON Object Error");
			e.getStackTrace();
		}
		try {
			for(int i=0;i<serverList.size();i++) {

				if(serverList.get(i).id == myServerId) {
					continue;
				}
				InetAddress ip=InetAddress.getByName(serverList.get(i).ipAddress);
				Socket socket = new Socket(ip, serverList.get(i).port);
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				//DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

				// transfer JSONObject as String to the server
				dataOutputStream.writeUTF(obj.toString());
				//socket.close();

			}
		} catch (Exception e) {
			System.out.println("Connection(s) failed");
		}
	}


	private void sendDisableToserverList(int dsid) {

		for(int i = 0; i < routingTableReadFromTopologyFile.length; i++) {
			for(int j = 0; j < routingTableReadFromTopologyFile[i].length; j++) {
				if(j == (dsid - 1)) {
					continue;
				}
				routingTableReadFromTopologyFile[j][dsid-1] = 9999;
				routingTableReadFromTopologyFile[dsid-1][j] = 9999;
			}
		}
		for(int x=0;x<serverList.size();x++) {
			if(serverList.get(x).id == myServerId) {
				serverList.get(x).neighborsIdAndCost.remove(dsid);
				for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
					for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
						serverList.get(x).routingTable[i][j] = routingTableReadFromTopologyFile[i][j];
						//System.out.println("routingTableReadFromTopologyFile["+i+"]["+j+"] = "+routingTableReadFromTopologyFile[i][j]); 
					}
				}
				break;
			}
		}
		
		JSONObject obj = new JSONObject();
		try {
			obj.put("operation", "disable");
			obj.put("disable_server_id", dsid);
		}
		catch(Exception e) {
			System.out.println("JSON Object Error");
			e.getStackTrace();
		}
		try {
			for(int i = 0; i < serverList.size(); i++) {
				
				if(serverList.get(i).id == myServerId) {
					continue;
				}
				InetAddress ip=InetAddress.getByName(serverList.get(i).ipAddress);
				Socket socket = new Socket(ip, serverList.get(i).port);
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

				dataOutputStream.writeUTF(obj.toString());

				//socket.close();

			}
		} catch (Exception e) {
			System.out.println("Connection(s) failed");
		}
		serverList.remove(dsid-1);
		nextHop.remove(dsid);
		doStep(serverList);	
	}


	private void sendRoutingTableToNeighbor(String ipAddressOfNeighbor, int portOfNeighbor) {

		JSONObject json = new JSONObject();
		try {
			json.put("operation", "step");
			json.put("sender_id", myServerId);
			for(int i = 0; i < serverList.size(); i++) {
				if(serverList.get(i).id == myServerId) {
					json.put("rt", serverList.get(i).routingTable);
					break;
				}
			}
		}
		catch(Exception e) {
			System.out.println("JSON Object Error");
			e.getStackTrace();
		}
		try {
			InetAddress ip = InetAddress.getByName(ipAddressOfNeighbor);
			Socket socket = new Socket(ip, portOfNeighbor);
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

			// transfer JSONObject as String to the server
			dataOutputStream.writeUTF(json.toString());
			//socket.close();
		} catch (Exception e) {
			System.out.println("Connection(s) failed");
		}
	}



	private void displayPackets(ArrayList<ServerDetails> serverList) {
		System.out.println("Number of packets received since last invocation: " + numPackets + "\n");
		numPackets = 0;
	}

	private void displayRoutingTable(ArrayList<ServerDetails> serverList) {
		//System.out.println("Node Table for Server " + myServerId + " is");
		// Below code will print out node table for the host server
		/*
		for (int i = 0; i < serverList.size()+numDisabledServers; i++) {
			if (serverList.get(i).id == myServerId) {
				System.out.print("  ");
				for (int n = 0; n < serverList.size()+numDisabledServers; n++) {
					System.out.print((n+1) + "\t");
				}
				System.out.println();
				for (int j = 0; j < serverList.get(i).routingTable.length; j++) {
					System.out.print((j+1) + " ");
					for (int k = 0; k < serverList.get(i).routingTable[j].length; k++) {
						System.out.print(serverList.get(i).routingTable[j][k] + "\t");
					}
					System.out.print("\n");
				}
				break;
			}
		}
		*/
		
		System.out.println("\nRouting Table is: ");
		//This will print out the routing table
		System.out.println("<Dest_ID> <Next_Hop> <Cost>");
		for (int i = 0; i < serverList.size(); i++) {
			if (serverList.get(i).id == myServerId) {
				//System.out.println("nextHop size = " + nextHop.size());
				//System.out.println(nextHop);
				for (int j = 1; j <= serverList.size()+numDisabledServers; j++) {
					if (nextHop.containsKey(j)) {
						System.out.println("    " + j + "\t    " + nextHop.get(j) + "\t    " + serverList.get(i).routingTable[myServerId-1][j-1]);
					}
				}
			break;
			}
		}
	}
	
	private ArrayList<ServerDetails> createRoutingTable(ArrayList<ServerDetails> serverList) {
		// fetch the server you need
		// assign routing table to it
		for (int i = 0; i < serverList.size(); i++) {
			serverList.get(i).routingTable = new int[serverList.size()+numDisabledServers][serverList.size()+numDisabledServers];
			if (serverList.get(i).id == myServerId) {

				for (int j = 0; j < serverList.get(i).routingTable.length; j++) {
					for (int k = 0; k < serverList.get(i).routingTable[j].length; k++) {
						if (j == k) {
							serverList.get(i).routingTable[j][k] = 0;
						} else {
							serverList.get(i).routingTable[j][k] = 9999;
						}
					}
				}
			} else {
				for (int j = 0; j < serverList.get(i).routingTable.length; j++) {
					for (int k = 0; k < serverList.get(i).routingTable[j].length; k++) {
						serverList.get(i).routingTable[j][k] = 9999;
					}
				}
			}
		}

		//Iterate through neighborsIdAndCost to assign each neighbor their link cost to myServerId
		for (int i = 0; i < serverList.size(); i++) {
			if (serverList.get(i).id == myServerId) {
				for (int j = 0; j < serverList.get(i).routingTable.length; j++) {
					if (j + 1 == myServerId) {
						Iterator<Map.Entry<Integer, Integer>> itr = serverList.get(i).neighborsIdAndCost.entrySet()
								.iterator();
						while (itr.hasNext()) {
							Map.Entry<Integer, Integer> entry = itr.next();
							serverList.get(i).routingTable[j][entry.getKey() - 1] = entry.getValue();
						}
						break;
					}
				}
				break;
			}
		}

		return serverList;
	}

	private ArrayList<ServerDetails> readTopologyFile(String fileName, ArrayList<ServerDetails> serverList) {
		int totalServersCount = 0;
		int totalNeighborsCount = 0;

		HashMap<Integer, Integer> newNeighborIdAndCost = new HashMap<>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line;
			if ((line = br.readLine()) != null) {
				totalServersCount = Integer.parseInt(line);
			} else {
				throw new Exception("Topology File Not Correctly Formatted!");
			}
			if ((line = br.readLine()) != null) {
				totalNeighborsCount = Integer.parseInt(line);
			} else {
				throw new Exception("Topology File Not Correctly Formatted!");
			}

			String[] splitLine;
			for (int i = 0; i < totalServersCount; i++) {
				if ((line = br.readLine()) != null) {
					splitLine = line.split(" ");
					if (splitLine.length != 3) {
						throw new Exception("Topology File Not Correctly Formatted!");
					} else {
						ServerDetails newServer = new ServerDetails();
						newServer.setId(Integer.parseInt(splitLine[0]));
						newServer.setIpAddress(splitLine[1]);
						newServer.setPort(Integer.parseInt(splitLine[2]));
						newServer.setNoOfPacketsReceived(0);
						serverList.add(newServer);
					}
				} else {
					throw new Exception("Topology File Not Correctly Formatted!");
				}
			}
			
			for (int i = 0; i < totalNeighborsCount; i++) {
				if ((line = br.readLine()) != null) {
					splitLine = line.split(" ");
					if (splitLine.length != 3) {
						throw new Exception("Topology File Not Correctly Formatted!");
					} else {
						myServerId = Integer.parseInt(splitLine[0]);
						newNeighborIdAndCost.put(Integer.parseInt(splitLine[1]), Integer.parseInt(splitLine[2]));
						nextHop.put(Integer.parseInt(splitLine[1]), Integer.parseInt(splitLine[1]));
					}
				} else {
					throw new Exception("Topology File Not Correctly Formatted!");
				}
			}
			
			
			//get Host server's port # from myServerId and begin server
			for (int i = 0; i < serverList.size(); i++) {
				if (serverList.get(i).id == myServerId) {
					myPort = serverList.get(i).port;
				}
			}
			myIP = InetAddress.getLocalHost().getHostAddress();
			serverSocket = new ServerSocket(myPort);
			bootup();
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
			// System.exit(1);
		}
		for (int i = 0; i < serverList.size(); i++) {
			if (serverList.get(i).getId() == myServerId) {
				serverList.get(i).neighborsIdAndCost = newNeighborIdAndCost;
			} else {
				HashMap<Integer, Integer> emptyHashMap = new HashMap<>();
				emptyHashMap.put(0, 0);
				serverList.get(i).neighborsIdAndCost = emptyHashMap;
			}
		}
		return serverList;
	}
	
public void bootup() throws IOException {
		
		//Allow the program to handle multithreading for multiple Clients
		new Thread(() -> {
			while(true) {
				try {
					//Accept any Client trying to connect to us
					
					Socket clientSocket = serverSocket.accept();
					//Start new thread for each individual Client
					new Thread(new Connection(clientSocket)).start();
					
				} catch (IOException e) {
					
				}
			}
		}).start();
	}

	class Connection implements Runnable {
	private Socket clientSocket;
	
	public Connection(Socket socket) {
		clientSocket = socket;
	}
	
	public void run() {
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
			//Read all messages intercepted from Clients
			while(true) {
				String line = in.readUTF().toString();
				if (line == null) {
					return;
				}
				JSONObject receivedMSG = new JSONObject(line);
				//Handle case when Client tries to connect to you
				switch(receivedMSG.get("operation").toString()) {
				case "step":
					System.out.println("RECEIVED A MESSAGE FROM SERVER " + receivedMSG.get("sender_id").toString() + "\n");
					handleStep(receivedMSG);
					numPackets++;
					break;
				
				case "update":
					System.out.println("RECEIVED A MESSAGE FROM SERVER " + receivedMSG.get("sender_id").toString() + "\n");
					String newCost = receivedMSG.get("cost").toString();
					int server1 = Integer.parseInt(receivedMSG.get("update_server_id_1").toString());
					int server2 = Integer.parseInt(receivedMSG.get("update_server_id_2").toString());

					if(newCost.equalsIgnoreCase("inf")) {
						routingTableReadFromTopologyFile[server2-1][server1-1] = 9999;
					}
					else {
						routingTableReadFromTopologyFile[server2-1][server1-1] = Integer.parseInt(newCost);
					}
					for(int x=0;x<serverList.size();x++) {
						if(serverList.get(x).id == myServerId) {

							for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
								for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
									serverList.get(x).routingTable[i][j] = routingTableReadFromTopologyFile[i][j];
								}
							}
							break;
						}
					}
					updateRoutingTable(serverList, routingTableReadFromTopologyFile);
					numPackets++;
					break;
					
				case "disable":
					int disable_server_id = Integer.parseInt(receivedMSG.get("disable_server_id").toString());
					
					if(disable_server_id == myServerId) {
						System.out.println("SERVER HAS BEEN DISABLED. SHUTTING DOWN...");
						System.exit(0);
					}
					for(int i = 0; i < routingTableReadFromTopologyFile.length; i++) {
						for(int j = 0; j < routingTableReadFromTopologyFile[i].length; j++) {
							if(j == (disable_server_id-1)) {
								continue;
							}
							routingTableReadFromTopologyFile[j][disable_server_id-1] = 9999;
							routingTableReadFromTopologyFile[disable_server_id-1][j] = 9999;
						}
					}
					for(int x=0;x<serverList.size();x++) {
						if(serverList.get(x).id == myServerId) {
							serverList.get(x).neighborsIdAndCost.remove(disable_server_id);

							for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
								for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
									serverList.get(x).routingTable[i][j] = routingTableReadFromTopologyFile[i][j];
								}
							}
							break;
						}
					}

					serverList.remove(disable_server_id-1);
					nextHop.remove(disable_server_id);
					numDisabledServers++;
					numPackets++;
					break;
				case "crash":
					int crashId = Integer.parseInt(receivedMSG.get("server_id").toString());
					System.out.println("Server " + crashId + " has crashed. Updating table...");
					for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
						for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
							if(j == (crashId-1)) {
								continue;
							}
							routingTableReadFromTopologyFile[j][crashId-1] = 9999;
							routingTableReadFromTopologyFile[crashId-1][j] = 9999;
						}
					}
					for(int x=0;x<serverList.size();x++) {
						if(serverList.get(x).id == myServerId) {
							serverList.get(x).neighborsIdAndCost.remove(crashId);

							for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
								for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
									serverList.get(x).routingTable[i][j] = routingTableReadFromTopologyFile[i][j];
								}
							}
							break;
						}
					}
					
					serverList.remove(crashId-1);
					nextHop.remove(crashId);
					numDisabledServers++;
					numPackets++;
					break;
				} // end switch
			}
		} catch (IOException e) {
			System.out.println("Connection has been dropped");
		}
	}
}

	private void handleStep(JSONObject json) {
		int[][] newRT = new int[serverList.size() + numDisabledServers][serverList.size() + numDisabledServers];
		JSONArray arr = json.getJSONArray("rt");
		for (int i = 0; i < arr.length(); i++) {
			JSONArray innerArr = (JSONArray) arr.get(i);
			for(int j = 0; j < innerArr.length(); j++) {
				newRT[i][j] = Integer.parseInt(innerArr.get(j).toString());
			}
		}
		
		for (int i = 0; i < serverList.size(); i++) {
			if (serverList.get(i).id == myServerId) {
				break;
			}
		}
		serverList = updateRoutingTable(serverList, newRT);
		return;
	}
	
	
	
	private void doStep(ArrayList<ServerDetails> serverList) {

		for (int i = 0; i < serverList.size(); i++) {
			if (serverList.get(i).id == myServerId) {
				Iterator<Map.Entry<Integer, Integer>> itr = serverList.get(i).neighborsIdAndCost.entrySet().iterator();
				while (itr.hasNext()) {
					String ipAddressOfNeighbor = "";
					int portOfNeighbor = 0;
					Map.Entry<Integer, Integer> entry = itr.next();

					// find ip of neighbor and send routing table to that neighbor
					for (int k = 0; k < serverList.size(); k++) {
						if (serverList.get(k).id == entry.getKey()) {
							ipAddressOfNeighbor = serverList.get(k).ipAddress;
							portOfNeighbor = serverList.get(k).port;
							break;
						}
					}
					try {
						sendRoutingTableToNeighbor(ipAddressOfNeighbor, portOfNeighbor);
					} catch (Exception e) {

					}
				}
				break;
			}
		}
	}

	private ArrayList<ServerDetails> updateRoutingTable(ArrayList<ServerDetails> serverList, int[][] nrt) {

		int[][] myOriginalRoutingTable = new int[serverList.size()+numDisabledServers][serverList.size()+numDisabledServers];
		int[][] myNewRoutingTable = new int[serverList.size()+numDisabledServers][serverList.size()+numDisabledServers];

		int i = 0;
		for (i = 0; i < serverList.size(); i++) {
			if (serverList.get(i).getId() == myServerId) {

				for(int j = 0; j < serverList.get(i).routingTable.length; j++) {
					for(int k = 0; k < serverList.get(i).routingTable[j].length; k++) {
						myOriginalRoutingTable[j][k] = serverList.get(i).routingTable[j][k];
						myNewRoutingTable[j][k] = serverList.get(i).routingTable[j][k];
					}
				}
				break;
			}
		}


		int[] neighbors = new int[serverList.get(i).neighborsIdAndCost.size()];
		Iterator<Map.Entry<Integer, Integer>> itr = serverList.get(i).neighborsIdAndCost.entrySet().iterator();
		int x = 0;
		while (itr.hasNext()) {
			Map.Entry<Integer, Integer> entry = itr.next();
			neighbors[x] = entry.getKey();
			x++;
		}
		
		for (int j = 0; j < myNewRoutingTable.length; j++) {
				for (int k = 0; k < myNewRoutingTable[j].length; k++) {
					if (j == k) {

					} else {
						if (myNewRoutingTable[j][k] < nrt[j][k]) {

						} else {
							myNewRoutingTable[j][k] = nrt[j][k];
						}
					}
				}
			//}
		}


		for (int j = 0; j < myNewRoutingTable.length; j++) {
			if (j + 1 == myServerId) {
				// update routing table
				int hop = 0;
				for (int k = 0; k < myNewRoutingTable[j].length; k++) {
					hop++;
					if (j == k) {

					} else {
						// this array stores all the distance to a partiular server
						int newCosts[] = new int[serverList.get(i).neighborsIdAndCost.size()];
						// calculate all new costs to the server
						for (int a = 0; a < neighbors.length; a++) {

							newCosts[a] = myNewRoutingTable[j][neighbors[a] - 1]
									+ myNewRoutingTable[neighbors[a] - 1][k];
						}
						// find the minimum cost from the array
						int minCost = 9999;
						for (int a = 0; a < newCosts.length; a++) {
							if (minCost > newCosts[a]) {
								minCost = newCosts[a];
								nextHop.put(hop, neighbors[a]);
							}
						}
						myNewRoutingTable[j][k] = minCost;
					}
				}
			}
		}

		// check if your routing table has changed
		Boolean didRoutingTableChange = false;
		for (int s = 0; s < serverList.get(i).routingTable.length; s++) {
			for (int t = 0; t < serverList.get(i).routingTable[s].length; t++) {
				if (myNewRoutingTable[s][t] != myOriginalRoutingTable[s][t]) {
					didRoutingTableChange = true;
					break;
				}
			}
		}
		
		
		if (didRoutingTableChange) {
			serverList.get(i).routingTable = myNewRoutingTable;
			// send routing table to neighbors
			//doStep(serverList);   // UNCOMMENT IF YOU WANT EVERY CHANGE IN ROUTING TABLE TO BE IMMEDIATELY UPDATED ON EVERY OTHER SERVER

		}
		return serverList;
	}


	class ScheduledTask extends TimerTask {

		public void run() {
			//now = new Date(); // initialize date
			//System.out.println("Time is :" + now); // Display current time
			System.out.println("AUTOMATICALLY SENDING ROUTING TABLE...\n");
			doStep(serverList);
		}
	}
}