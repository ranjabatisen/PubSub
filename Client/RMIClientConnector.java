
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.Policy;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.*;

public class RMIClientConnector {

	private static Logger logger = Logger.getLogger(RMIClientConnector.class.getName());
	// making it a class variable because at a time a client can subscribe to only
	// one server
	private DatagramSocket clientSocket = null;
	private Communicate serverStub = null;
	private Thread clientUDPSubscriptionChannel = null;

	private String clientIp = null;
	private int clientPort = 0;
	private Registry registry = null;
	private String regisIp = null;
	private Thread rmiServerPing = null;
	private boolean isClientConnectedToServer = false;

	public RMIClientConnector(String clientIp, int clientPort, String regisIp) {
		this.clientIp = clientIp;
		this.clientPort = clientPort;
		this.regisIp = regisIp;
		try {
			
			List<GroupServer> groupServers = getServersList();
			fetchLiveServerStub(groupServers);
			rmiServerPing = pingServer();
			isClientConnectedToServer = true;
			clientSocket = new DatagramSocket(clientPort);
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	// default server stub for testing
	public void setDummyServerStub() {
		serverStub = null;
		try {
			serverStub = (Communicate) registry.lookup("rmi://localhost/RMISERVER");
		} catch (RemoteException | NotBoundException e) {
			e.printStackTrace();
		}
	}


	public boolean ping() {
		boolean resp = false;
		if (serverStub == null) {
			logger.info("No active server stub available to ping");
		}
		try {
			resp = serverStub.ping();
		} catch (RemoteException e) {
			logger.severe("RemoteException while trying to ping");
			e.printStackTrace();
		}
		return resp;
	}

	public boolean join() {
		boolean resp = false;
		logger.info("Sending request to join the server");
		if (serverStub == null) {
			logger.info("No active server stub available to join");
		}
		try {
			resp = serverStub.join(clientIp, clientPort);
		} catch (RemoteException e) {
			logger.severe("RemoteException while trying to join");
			e.printStackTrace();
		}
		logger.info("join Response from server is "+resp);
		return resp;
	}

	public boolean subscribe(String article) {
		return subscribeToServer(article) != null;
	}

	private Thread subscribeToServer(String article) {

		boolean response = false;
		try {
			logger.info("client: " + clientIp + ", " + clientPort + " sending subscription request to server. Article: " + article);
			if(null == serverStub) {
				logger.severe("Subscribe failed : Client is not connected the server");
				return null;
			}
			response = serverStub.subscribe(clientIp, clientPort, article);
			if (clientUDPSubscriptionChannel== null && response ) {
				clientUDPSubscriptionChannel = openSubscriptionChannel();
			}
		} catch (RemoteException e) {
			logger.severe("Remote Exception while subscribing to Server");
			e.printStackTrace();
		} catch (SocketException e) {
			logger.severe("SocketException while subscribing to Server");
			e.printStackTrace();
		}
		logger.info("Subscription response from server: " + response);
		return clientUDPSubscriptionChannel;
	}

	public boolean publish(String article) {
		boolean response = false;
		try {
			logger.info("client: " + clientIp + ", " + clientPort + " sending publish request to server. Article: " + article);
			response = serverStub.publish(clientIp, clientPort, article);

		} catch (RemoteException e) {
			logger.severe("Remote Exception while publishing to Server");
			e.printStackTrace();
		}
		logger.info("Publish response from server: " + response);
		return response;

	}
	private Thread pingServer() {
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					boolean isActive = false;
					while (true) {
						if(!isClientConnectedToServer) {
							return;
						}
						if(serverStub!=null) {
							try {
								isActive = serverStub.ping();
							} catch (Exception e) {
								logger.info("Server connection refused. Trying another server ");
								//<GroupServer> groupServers = getServersList();
								List<GroupServer> groupServers = getServersList();
								while(groupServers.size()==0) {
									Thread.sleep(1000);
								}
								fetchLiveServerStub(groupServers); 
								logger.info("Failovered to another server");

							}
						}else {
							List<GroupServer> groupServers = getServersList();
							fetchLiveServerStub(groupServers); 
						}
						if(isActive) {
							Thread.sleep(5000);
						}
						
					}

				} catch (InterruptedException e) {
					logger.info("Sleeping at get Active Srever Stub");
					e.printStackTrace();
				} 
			}
		});
		t1.start();
	
		return t1;
	}

	public boolean unsubscribe(String article) {
		boolean response = false;
		try {
			logger.info("client: " + clientIp + ", " + clientPort + " sending unscubscribe request to server. Article: " + article);
			response = serverStub.unsubscribe(clientIp, clientPort, article);

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("Unsubscription response from server: " + response);
		return response;
	}

	public boolean leave() throws RemoteException {
		logger.info("client: " + clientIp + ", " + clientPort  + " sending leave request to server");
		boolean response = serverStub.leave(clientIp, clientPort);
		logger.info("Leave response from server: " + response);
		if(response == true) {
			isClientConnectedToServer = false;
			clientSocket.close();
			serverStub = null;
			
		}
		
		if(clientUDPSubscriptionChannel != null) {
			clientUDPSubscriptionChannel.stop();
		}
		return response;
	}

	private void fetchLiveServerStub(List<GroupServer> groupServers) {


		if (groupServers == null || groupServers.size() == 0) {
			logger.info("No group server to connect");
		}
		for (GroupServer grpServer : groupServers) {
			try {

				registry = LocateRegistry.getRegistry(grpServer.ip, 1099);

				this.serverStub = (Communicate) registry.lookup(grpServer.bindingName);

				if (this.serverStub.ping()) {

					this.join();
				}

			} catch (RemoteException | NotBoundException e) {
				logger.severe("exception at fetchLiveServerStub: " + e.getMessage());
			}
		}
		logger.info("Returning server stub ");

	}

	private Thread openSubscriptionChannel() throws SocketException {

		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					logger.info("Opening subscription channel");
					while (clientSocket != null && !clientSocket.isClosed()) {

						byte[] buf = new byte[1024];
						// TODO update InetAddress to be used
						DatagramPacket packet = new DatagramPacket(buf, buf.length);

						clientSocket.receive(packet);

						String receivedArticle = new String(packet.getData(), 0, packet.getLength());
						logger.info("Received Article from Server via UDP: " + receivedArticle);
						
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		t1.start();
		return t1;
	}

	/**
	 * Connect to registry server via UDP
	 * 
	 * @return List of Group Server Stubs
	 */
	private List<GroupServer> getServersList() {

		try {
			
			int port = 5105; // hardcoding 5105 - port of registry server

			byte[] buf = new byte[1024];
			DatagramSocket socket = new DatagramSocket();

			String request = "GetList;RMI;" + clientIp + ";" + port;
			buf = request.getBytes();

			
			InetAddress address = InetAddress.getByName(regisIp);
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
			socket.send(packet);

			logger.info("GetList() request sent to registry server");

			socket.receive(packet);
			String received = new String(packet.getData(), 0, packet.getLength());
			logger.info("Received from Registry Server via UDP: " + received);
			socket.close();
			return getGroupServers(received);

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
	}


	//[IP;BindingName;Port;IP;BindingName;Port;IP;BindingName;Port ... and so on]
	private List<GroupServer> getGroupServers(String received) {
		
		List<GroupServer> groupServers = new ArrayList<GroupServer>();
		if (received != null && !received.isEmpty()) {
			String[] split = received.split(";");
			int count = 0;
			
			for (int i = 0; i < split.length-1; i = i+2) {
				//Hardcoding BindName since 
				String bindingName = "RMISERVER";
				GroupServer grpServer = new GroupServer(split[i], bindingName, Integer.valueOf(split[i+1]) );
				groupServers.add(grpServer);
				//i = i+1;
			}
		}
		return groupServers;
	}

	class GroupServer{
		String ip;
		String bindingName;
		int port;
		GroupServer(String ip,String bindingName, int port){
			this.ip= ip;
			this.bindingName = bindingName;
			this.port = port;
			
		}
	}
}
