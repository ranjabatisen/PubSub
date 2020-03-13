
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;

public class RMIClientExecutor {
	static String clientIp = null;
	static int clientPort = 4567; // client's RMI port will be overridden as per args[]
	static String regisIp = "";
	
	/**
	 * 
	 * @param args
	 * args[0] client Ip
	 * args[1] client port
	 * args[2] registry server's ip
	 * args[3] method name
	 * args[4] article content only for subscribe, unsubscribe, publish
	 * 
	 * 
	 */
	public static void main(String args[]) {
		
		String article = null;
		String clientIp2 = null;
		String clientIp3 = null;
			clientIp = args[0];
			clientPort = Integer.valueOf(args[1]);
			regisIp = args[2];
			RMIClientConnector rmiClientConnector = new RMIClientConnector(clientIp, clientPort, regisIp);
			String op = args[3];
			
		
			if(op.equals("subscribe")|| op.equals("unsubscribe")||op.equals("publish")) {
				article = args[4];
			}
			
			boolean response = runOp(op, rmiClientConnector, article, clientIp2, clientIp3, regisIp);
			
			
	}
	
	public static boolean runOp(String op, RMIClientConnector rmiClientConnector, String article, String clientIp2, String clientPort2, 
			String regisIp) {
		boolean response = false;
		switch(op) {
		case "join":
			response = rmiClientConnector.join();
			break;
		case "ping":
			response = rmiClientConnector.ping();
			break;
		case "subscribe":
			response = rmiClientConnector.subscribe(article);
			break;
		case "unsubscribe":
			
			response = rmiClientConnector.unsubscribe(article);
			
			break;
		case "publish":
			response = rmiClientConnector.publish(article);
			break;
		case "leave":
			try {
				
				response = rmiClientConnector.leave();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Subscribing to server to check whether leave is successful or not ");

				rmiClientConnector.subscribe("Science;Origantor;UMN;");
				
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			break;
		 default:
			System.out.println("Invalid method to client");
			break;
		}

		return response;
	}
}
