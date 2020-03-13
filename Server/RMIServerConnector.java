
/**
 * 
 */
import java.util.logging.*;

import com.sun.javafx.geom.transform.BaseTransform.Degree;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.IOException; 
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 
import java.net.InetAddress;

/**
 * @author ranjabatisen
 *
 */
public class RMIServerConnector {
	private static int serverRMIPort = 5106;
	private static int registryServerPort = 5105;
	private static Logger logger = Logger.getLogger(RMIServerConnector.class.getName());
        
        public static void Register(DatagramSocket ds, InetAddress ip, String grpSvIp) throws IOException
        {
            byte buf[] = null; 
            
            
            String inp = "Register;RMI;"+grpSvIp+";"+serverRMIPort+";RMISERVER;1099"; 
  
            // convert the String input into the byte array. 
            buf = inp.getBytes(); 
  
            // Create the datagramPacket for sending the data. 
            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, registryServerPort); 
  
            // Invoke the send call to actually send the data. 
            ds.send(DpSend); 
        }
        public static void Deregister(DatagramSocket ds, InetAddress ip, String grpSvIp) throws IOException
        {
            byte buf[] = null; 
   
            // Storing ip address of group server
            
            String inp = "Deregister;RMI;"+grpSvIp+";"+serverRMIPort+";RMISERVER;1099"; 
  
            // convert the String input into the byte array. 
            buf = inp.getBytes(); 
  
            // Create the datagramPacket for sending the data. 
            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, registryServerPort); 
  
            // invoke the send call to actually send the data. 
            ds.send(DpSend); 
            logger.info("Deregister request sent to Registry server");
        }
	
	public static void main(String[] args) throws IOException{
                String grpSvIp = args[0];
                String regisIp = args[1];
				System.setProperty("java.rmi.server.hostname", grpSvIp);
                // Create a datagram socket for UDP connection
                DatagramSocket ds = new DatagramSocket(serverRMIPort); 
                
                // Storing ip address of registry server
                InetAddress ip = InetAddress.getByName(regisIp);
                
                // Registering with registry server
                
                Register(ds,ip, grpSvIp);
                
                
		try {
			logger.info("RMI Server is creating remote object");

			Registry registry = LocateRegistry.createRegistry(1099);
	        String[] objects = registry.list();
	        

			RMIServer rmiServer = new RMIServer();
			
			Communicate stub = (Communicate) UnicastRemoteObject.exportObject(rmiServer, 0);
		    registry.rebind("RMISERVER", stub);

			logger.info("RMI Server is ready.");

		} catch (Exception e) {
			logger.severe("RMI Server failed: " + e);
		}
                Thread t1 = new Thread(new Runnable(){
                @Override
                public void run() 
                { 
                    try {
                    	int count=0;
                    	boolean isDeRegister =false;
                        if(args.length == 3 && args[2].equalsIgnoreCase("deregister"))
                        {
                        	isDeRegister = true;
                        }
                        while(true)
                        {
                            byte[] receive = new byte[64]; 
  
                            DatagramPacket DpReceive = null; 
                            
                            count++;
                            
                            // Create a DatgramPacket to receive the data. 
                            DpReceive = new DatagramPacket(receive, receive.length); 
  
                            // Receiving heartbeat from registry server
                            ds.receive(DpReceive); 
                    
                            logger.info("Message received from registry server: " + data(receive));
  
                            // Sending heartbeat back to registry server
                            ds.send(DpReceive);
                            
                            if(isDeRegister && count == 5) {
                            	Deregister(ds,ip, grpSvIp);
                            	return;
                            }
                        }
                    } 
                    catch (IOException e) { 
                        System.out.println (e.toString()); 
                    } 
                }
            });
            t1.start();
	}
        // Converting bytes received from registry server to string
        public static StringBuilder data(byte[] a) 
        { 
            if (a == null) 
                return null; 
            StringBuilder ret = new StringBuilder(); 
            int i = 0; 
            while (a[i] != 0) 
            {    
                ret.append((char) a[i]); 
                i++; 
            } 
            return ret; 
        }
}
