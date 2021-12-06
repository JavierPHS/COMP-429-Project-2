package test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import org.json.JSONArray;
import org.json.JSONObject;


class Message{
	String operation;
	HashMap<Integer, Integer> link=new HashMap<>();
	int[][] rt;
}



public class Main {

	public ArrayList<ServerDetails> serverList =new ArrayList<>();
	int[][] routingTableReadFromTopologyFile;
	int updateInterval=1000;
	int myServerId;
	int myPort;
	String myIP;
	ServerSocket serverSocket;
	int numDisabledServers=0;
	

	public static void main(String[] args) {

		System.out.println("Beginning Program...");
		Main main = new Main();
		main.menu();
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
				//serverList = readTopologyFile("topology.txt", serverList);
				

				serverList = createRoutingTable(serverList);
				
				//Main mainObj = new Main();
				
				//Timer time = new Timer(); // Instantiate Timer Object
        		//ScheduledTask st = mainObj.new ScheduledTask(); // Instantiate SheduledTask class
        		//time.schedule(st,60000, updateInterval); // Create Repetitively task for every 1 secs

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

				/*
				 * // Fire off the server listening thread ip = InetAddress.getLocalHost();
				 * Runnable server = new Server(localServerInfo.Port); new
				 * Thread(server).start();
				 */

				/*
				 * // Fire off the timer that sends a update every 10 seconds Timer timer = new
				 * Timer(true); UpdateTimerTask updateTimertask = new UpdateTimerTask();
				 * timer.scheduleAtFixedRate(updateTimertask, updateDelay, updateDelay);
				 */
				System.out.println(splitLine[0] + " SUCCESS");
				//sendRoutingTableToNeighbor("send", "192.168.0.44", 8200);
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
				//sendRoutingTableToNeighbor("step","192.168.0.44", 6666);
				System.out.println(splitLine[0] + " SUCCESS");
				break;
			case "packets":
				displayPackets(serverList);
				System.out.println(splitLine[0] + " SUCCESS");
				break;
			case "display":
				displayRoutingTable(serverList);
				System.out.println(splitLine[0] + " SUCCESS");
				break;
			case "disable":
				//send this to all servers, not just neighbors
				if(Integer.parseInt(splitLine[1])==(myServerId))
				{
					System.out.println("Can not Disable yourself");
					break;
				}
				sendDisableToserverList(Integer.parseInt(splitLine[1]));
				numDisabledServers++;
				break;
			case "crash":
				sendCrashToserverList();
				System.out.println(splitLine[0] + " SUCCESS");
				System.exit(1);
				break;
			default:
				break;
			}

		}
	}


	private void sendUpdateLinkCostToNeighbor(int linkServer1, int linkServer2, String newCostOfLink) {
		// TODO Auto-generated method stub


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

		}
		catch(Exception e) {
			System.out.println("JSON Object Error");
			e.getStackTrace();
		}
		try {
			for(int i=0;i<serverList.size();i++) {

				///System.out.println("inside Try");
				InetAddress ip=InetAddress.getByName(serverList.get(i).ipAddress);
				Socket socket = new Socket(ip, serverList.get(i).port);
				//System.out.println("socket Created");
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				//System.out.println("inside Send, dataSteam Created");

				// transfer JSONObject as String to the server
				//dataOutputStream.writeUTF(stringJsonData);
				dataOutputStream.writeUTF(obj.toString());

				//System.out.println("inside Send, wrote Data to neighbor");
				//socket.close();

			}
		} catch (Exception e) {
			//System.out.println("hi");
			e.printStackTrace();
		}
		
		doStep(serverList);
	}


	private void sendCrashToserverList() {
		// TODO Auto-generated method stub
		JSONObject obj=new JSONObject();
		try {
			obj.put("operation", "crash");
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

				//System.out.println("inside Try");
				InetAddress ip=InetAddress.getByName(serverList.get(i).ipAddress);
				Socket socket = new Socket(ip, serverList.get(i).port);
				//System.out.println("socket Created");
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				//System.out.println("inside Send, dataSteam Created");

				// transfer JSONObject as String to the server
				//dataOutputStream.writeUTF(stringJsonData);
				dataOutputStream.writeUTF(obj.toString());

				//System.out.println("inside Send, wrote Data to neighbor");
				//socket.close();

			}
		} catch (Exception e) {
			//System.out.println("hi");
			e.printStackTrace();
		}
	}


	private void sendDisableToserverList(int dsid) {

		for(int i=0;i<routingTableReadFromTopologyFile.length;i++) {
			for(int j=0;j<routingTableReadFromTopologyFile[i].length;j++) {
				if(j == (dsid-1)) {
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
					}
				}
				break;
			}
		}


		// TODO Auto-generated method stub
		JSONObject obj=new JSONObject();
		try {
			obj.put("operation", "disable");
			obj.put("disable_server_id", dsid);
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

				//System.out.println("inside Try");
				InetAddress ip=InetAddress.getByName(serverList.get(i).ipAddress);
				Socket socket = new Socket(ip, serverList.get(i).port);
				//System.out.println("socket Created");
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				//System.out.println("inside Send, dataSteam Created");

				// transfer JSONObject as String to the server
				//dataOutputStream.writeUTF(stringJsonData);
				dataOutputStream.writeUTF(obj.toString());

				//System.out.println("inside Send, wrote Data to neighbor");
				//socket.close();

			}
		} catch (Exception e) {
			//System.out.println("hi");
			e.printStackTrace();
		}
		serverList.remove(dsid-1);
		doStep(serverList);	
	}


	private void sendRoutingTableToNeighbor(String ipAddressOfNeighbor, int portOfNeighbor) {
		// TODO Auto-generated method stub

		JSONObject json =new JSONObject();
		try {
			json.put("operation", "step");
			json.put("sender_id", myServerId);
			for(int i = 0; i < serverList.size(); i++) {
				//System.out.println("server id ="+serverList.get(i).id);
				if(serverList.get(i).id == myServerId) {
					//System.out.print("matched = "+serverList.get(i).routingTable);
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
			//System.out.println("inside Try");
			InetAddress ip = InetAddress.getByName(ipAddressOfNeighbor);
			Socket socket = new Socket(ip, portOfNeighbor);
			//System.out.println("socket Created");
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
			//DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
			//System.out.println("inside Send, dataSteam Created");

			// transfer JSONObject as String to the server
			//dataOutputStream.writeUTF(stringJsonData);
			dataOutputStream.writeUTF(json.toString());

			//System.out.println("inside Send, wrote Data to neighbor");
			//socket.close();
		} catch (Exception e) {
			//System.out.println("hi");
			e.printStackTrace();
		}
	}



	private void displayPackets(ArrayList<ServerDetails> serverList) {
		for (int i = 0; i < serverList.size(); i++) {
			if (serverList.get(i).id == myServerId) {
				System.out.println(serverList.get(i).noOfPacketsReceived);
			}
		}
	}

	private void displayRoutingTable(ArrayList<ServerDetails> serverList) {
		System.out.println("Routing Table for Server " + myServerId + " is");
		
		// Below code will print out node table for the host server
		
		for (int i = 0; i < serverList.size(); i++) {
			if (serverList.get(i).id == myServerId) {
				System.out.print("  ");
				for (int n = 0; n < serverList.size(); n++) {
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
		
		System.out.println();
		//This will print out the routing table
		System.out.println("<Destination_ID>\t<Next_Hop>\t<Cost>");
	}
	private ArrayList<ServerDetails> createRoutingTable(ArrayList<ServerDetails> serverList) {
		// fetch the server you need
		// assign routing table to the
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
		/*for (int i = 0; i < serverList.size(); i++) {
			System.out.println("Routing Table is");
			if (serverList.get(i).id == myServerId) {
				for (int j = 0; j < serverList.get(i).routingTable.length; j++) {
					for (int k = 0; k < serverList.get(i).routingTable[j].length; k++) {
						System.out.print(serverList.get(i).routingTable[j][k] + " ");
					}
					System.out.print("\n");
				}
				break;
			}
		}*/

		return serverList;
	}

	private ArrayList<ServerDetails> readTopologyFile(String fileName, ArrayList<ServerDetails> serverList) {

		//System.out.println(fileName);
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
					System.out.println(line);  //////////////////// FOR DEBUGGING PURPOSES
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
						System.out.println(line); /////////////////// FOR DEBUGGING PURPOSES
						myServerId = Integer.parseInt(splitLine[0]);
						newNeighborIdAndCost.put(Integer.parseInt(splitLine[1]), Integer.parseInt(splitLine[2]));
					}
				} else {
					throw new Exception("Topology File Not Correctly Formatted!");
				}
			}
			
			System.out.println("myServerId is " + myServerId);  /////////////// FOR DEBUGGING
			
			//get Host server's port # from myServerId and begin server
			for (int i = 0; i < serverList.size(); i++) {
				if (serverList.get(i).id == myServerId) {
					myPort = serverList.get(i).port;
				}
			}
			myIP = InetAddress.getLocalHost().getHostAddress();
			serverSocket = new ServerSocket(myPort);
			System.out.println("My port is " + myPort); ///////////////// FOR DEBUGGING
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
		//System.out.println("out of readTopology File");
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
					System.out.println("RECEIVED A MESSAGE FROM SERVER " + receivedMSG.get("sender_id").toString());
					handleStep(receivedMSG);
					break;
				
				case "update":
					
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

					break;
					
				case "disable":

					int disable_server_id = Integer.parseInt(receivedMSG.get("disable_server_id").toString());

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
					/*
						for(int i=0;i<serverList.size();i++) {
							if(serverList.get(i).id == myServerId) {
								serverList.get(i).neighborsIdAndCost.remove(disable_server_id);
								//Do I make changes to routing table?
								for(int j=0;j<serverList.get(i).routingTable.length;j++) {
									if(j == (disable_server_id-1)) {
										continue;
									}
									serverList.get(i).routingTable[j][disable_server_id-1] = 9999;
									serverList.get(i).routingTable[disable_server_id-1][j] = 9999;
								}
								break;
							}
						}*/

					serverList.remove(disable_server_id-1);
					numDisabledServers++;
					break;
				case "crash":
					System.out.println("CRASH SUCCESSFUL");
					System.exit(1);
					break;
				}
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
				serverList.get(i).noOfPacketsReceived++;
				break;
			}
		}
		serverList = updateRoutingTable(serverList, newRT);
		return;
	}
	
	
	
	private void doStep(ArrayList<ServerDetails> serverList) {

		for (int i = 0; i < serverList.size(); i++) {
			if (serverList.get(i).id == myServerId) {
				//System.out.println("my servers id = "+serverList.get(i).id);

				Iterator<Map.Entry<Integer, Integer>> itr = serverList.get(i).neighborsIdAndCost.entrySet().iterator();
				while (itr.hasNext()) {
					String ipAddressOfNeighbor = "";
					int portOfNeighbor = 0;
					Map.Entry<Integer, Integer> entry = itr.next();

					//System.out.println("neighbor = "+entry.getKey());
					//System.out.println("value = "+entry.getValue());
					// find ip of neighbor and send routing table to that neighbor
					for (int k = 0; k < serverList.size(); k++) {
						if (serverList.get(k).id == entry.getKey()) {
							ipAddressOfNeighbor = serverList.get(k).ipAddress;
							portOfNeighbor = serverList.get(k).port;
							break;
						}
					}
					//System.out.println("ipaddress of neighbor = " + ipAddressOfNeighbor);
					//System.out.println("port of neighbor = " + portOfNeighbor);
					try {
						//System.out.println("send message");
						sendRoutingTableToNeighbor(ipAddressOfNeighbor, portOfNeighbor);
						//System.out.println("message sent");
					} catch (Exception e) {

					}
				}
				break;
			}
		}
	}

	private ArrayList<ServerDetails> updateRoutingTable(ArrayList<ServerDetails> serverList, int[][] nrt) {

		/*displayRoutingTable(serverList);
		System.out.println("inside update Routing Table, Table = ");
		for (int s = 0; s < nrt.length; s++) {
			for (int t = 0; t < nrt[s].length; t++) {
				System.out.print(nrt[s][t]);
			}
			System.out.println();
		}*/


		int[][] myOriginalRoutingTable = new int[serverList.size()+numDisabledServers][serverList.size()+numDisabledServers];
		int[][] myNewRoutingTable = new int[serverList.size()+numDisabledServers][serverList.size()+numDisabledServers];

		/*System.out.println("2nd time = my Orginial Routing Table = ");
		System.out.println("my Orginial Routing Table = ");
		for (int s = 0; s < myOriginalRoutingTable.length; s++) {
			for (int t = 0; t < myOriginalRoutingTable[s].length; t++) {
				System.out.print(myOriginalRoutingTable[s][t]);
			}
			System.out.println();
		}
		for (int s = 0; s < myNewRoutingTable.length; s++) {
			for (int t = 0; t < myNewRoutingTable[s].length; t++) {
				System.out.print(myNewRoutingTable[s][t]);
			}
			System.out.println();
		}*/
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

		/*System.out.println("3rd time = my Orginial Routing Table = ");
		for (int s = 0; s < myOriginalRoutingTable.length; s++) {
			for (int t = 0; t < myOriginalRoutingTable[s].length; t++) {
				System.out.print(myOriginalRoutingTable[s][t]);
			}
			System.out.println();
		}
		for (int s = 0; s < myNewRoutingTable.length; s++) {
			for (int t = 0; t < myNewRoutingTable[s].length; t++) {
				System.out.print(myNewRoutingTable[s][t]);
			}
			System.out.println();
		}*/

		int[] neighbors = new int[serverList.get(i).neighborsIdAndCost.size()];
		Iterator<Map.Entry<Integer, Integer>> itr = serverList.get(i).neighborsIdAndCost.entrySet().iterator();
		int x = 0;
		while (itr.hasNext()) {
			Map.Entry<Integer, Integer> entry = itr.next();
			neighbors[x] = entry.getKey();
			x++;
		}
		//System.out.println(
		//		"No. of Neighbors of server " + myServerId + " = " + serverList.get(i).neighborsIdAndCost.size());
		for (int j = 0; j < myNewRoutingTable.length; j++) {
			/* if (j + 1 == myServerId) {

			} else {
			*/
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

		/*System.out.println("4th time = my Orginial Routing Table = ");
		for (int s = 0; s < myOriginalRoutingTable.length; s++) {
			for (int t = 0; t < myOriginalRoutingTable[s].length; t++) {
				System.out.print(myOriginalRoutingTable[s][t]);
			}
			System.out.println();
		}
		for (int s = 0; s < myNewRoutingTable.length; s++) {
			for (int t = 0; t < myNewRoutingTable[s].length; t++) {
				System.out.print(myNewRoutingTable[s][t]);
			}
			System.out.println();
		}*/

		for (int j = 0; j < myNewRoutingTable.length; j++) {
			if (j + 1 == myServerId) {
				// update routing table
				for (int k = 0; k < myNewRoutingTable[j].length; k++) {
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
							}
						}
						myNewRoutingTable[j][k] = minCost;
					}
				}
			}
		}
		/*System.out.println("5th time, my Orginial Routing Table = ");
		for (int s = 0; s < myOriginalRoutingTable.length; s++) {
			for (int t = 0; t < myOriginalRoutingTable[s].length; t++) {
				System.out.print(myOriginalRoutingTable[s][t]);
			}
			System.out.println();
		}
		for (int s = 0; s < myNewRoutingTable.length; s++) {
			for (int t = 0; t < myNewRoutingTable[s].length; t++) {
				System.out.print(myNewRoutingTable[s][t]);
			}
			System.out.println();
		}*/
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
		//System.out.println("did Routing Table change = "+didRoutingTableChange);
		//System.out.println("New Routing Table = "+myNewRoutingTable);
		
		// UNCOMMENT BELOW IF YOU WANT EVERY CHANGE IN ROUTING TABLE TO BE IMMEDIATELY UPDATED ON EVERY OTHER SERVER
		/*
		if (didRoutingTableChange) {
			serverList.get(i).routingTable = myNewRoutingTable;
			// send routing table to neighbors
			doStep(serverList);

		}
		*/
		
		//serverList.get(i).noOfPacketsReceived++;
		// displayRoutingTable(serverList);
		return serverList;
	}


	class ScheduledTask extends TimerTask {

		//Date now; // to display current time

		// Add your task here
		public void run() {
			//now = new Date(); // initialize date
			//System.out.println("Time is :" + now); // Display current time
			doStep(serverList);
		}
	}
}