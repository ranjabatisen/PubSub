/**
 * 
 */


/**
 * @author ranjabatisen
 *
 */
import java.rmi.*;
public interface Communicate extends Remote {
	   
	//public int add(int a,int b) throws RemoteException;
	public boolean join(String ip,int port) throws RemoteException;
	
	public boolean leave (String ip, int port) throws RemoteException;
	
	public boolean subscribe (String ip, int port, String article) throws RemoteException;

	public boolean unsubscribe (String ip, int port, String article) throws RemoteException;
	
	public boolean publish (String ip, int port, String article) throws RemoteException;
	
	public boolean ping () throws RemoteException;

}
