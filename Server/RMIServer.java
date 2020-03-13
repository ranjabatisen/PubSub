
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.Set;
import java.util.logging.*;

/**
 * 
 */

/**
 * @author ranjabatisen
 *
 */

public class RMIServer implements Communicate {
	private static Logger logger = Logger.getLogger(RMIServer.class.getName());

	private Map<String, Integer> clientIpPortMap;
	private Map<String, Set<String>> articleSubscriberMap;
	private Set<String> publishedArticles;
	
	private List<String> types;

	private static final int MAXSTRING = 120;
	private static final int MAXCLIENT = 5;
	private static final String DELIM_ARTICLE = ";";

	public RMIServer() throws RemoteException {
		intializeTypeList();
		clientIpPortMap = new HashMap<String, Integer>();
		articleSubscriberMap = new HashMap<String, Set<String>>();

	}

	private void intializeTypeList() {
		types = new ArrayList<String>();
		types.add("Sports");
		types.add("Lifestyle");
		types.add("Entertainment");
		types.add("Business");
		types.add("Technology");
		types.add("Science");
		types.add("Politics");
		types.add("Health");
		
	}

	public Map<String, Integer> getClientIpPortMap() {
		return clientIpPortMap;
	}

	public Map<String, Set<String>> getArticleSubscribersMap() {
		return articleSubscriberMap;
	}

	/*
	 * Method called when client send requests to connect the server
	 */

	public boolean join(String ip, int port) throws RemoteException {

		clientIpPortMap.put(ip, port);

		if (clientIpPortMap.containsKey(ip) && clientIpPortMap.size() <= MAXCLIENT ) {
			logger.info("Client "+ip+"joined successfully to the server");
			return true;
		} else {
			logger.info("Client "+ip+"could not join to the server");
			return false;
		}
	}

	/*
	 * Method called when client sends request to disconnect from a server
	 */
	public boolean leave(String ip, int port) throws RemoteException {

		boolean result = true;
		if (clientIpPortMap.remove(ip) != null) {

			result = removeClientSubscriptions(ip);
			if(result == true) {
				logger.info("Client "+ip+"left the server");

			} else {
				logger.info("Client "+ip+" is not able to leave the server");
			}
		}
		return result;
	}

	/**
	 * Method to remove client subscriptions
	 * 
	 * @param Client iP
	 * @return If subscription is successfully removed or not
	 */
	private boolean removeClientSubscriptions(String ip) {

		boolean result = true;

		Iterator<Entry<String, Set<String>>> itr = articleSubscriberMap.entrySet().iterator();

		try {
			while (itr.hasNext()) {
				Entry<String, Set<String>> entry = itr.next();
				if (entry.getValue().contains(ip)) {
					entry.getValue().remove(ip);
				}
			}
		} catch (Exception e) {

			result = false;
			e.printStackTrace();
		}
		
		return result;

	}

	/**
	 * Method invoked by a client to subscribe to a particular article
	 * 
	 */
	public boolean subscribe(String ip, int port, String article) throws RemoteException {

		// check whether client has registered to the group server else return false

		boolean result = true;
		String[] topics;
		logger.info("Article subscribe request received from " + ip + ", " + port + " for article: " + article);
		if (article.isEmpty() || !clientIpPortMap.containsKey(ip)) {
			result = false;
		} else {
			// add each topic to article subscriber map by breaking down the article string

			if (!isValidSubscription(article)) {
				logger.info("Invalid Article to subscribe. Subscription request failed");
				result = false;
			} else {
				topics = getSplitArticle(article);
				result = addToSubscriberMap(ip, topics);
			}
		}
		logger.info("Article subscription status: " + result);
		return result;
	}

	private String[] getSplitArticle(String article) {

		// consider only first three fields and ignore content for subscription
		String[] split = article.split(DELIM_ARTICLE);

		return split;
	}

	private boolean addToSubscriberMap(String iP, String[] topics) {
		boolean result = true;

		if (topics.length == 0) {
			result = false;
		} else {

			for (String topic : topics) {
				Set<String> subscribers = articleSubscriberMap.getOrDefault(topic, new HashSet<String>());
				subscribers.add(iP);
				articleSubscriberMap.put(topic, subscribers);
			}
		}

		return result;
	}

	/**
	 * Method to check validity of the article
	 * 
	 * @param article array has type, originator, org, content strings
	 * @return whether given article is valid or not
	 */
	private boolean isValidSubscription(String article) {

		boolean result = true;

		if (!isLengthValid(article)) {
			result = false;
		} else if (article.lastIndexOf(";") != article.length() - 1) {
			// no content field is allowed for subscription
			result = false;
		} else if (!isValidTopic(article)) {
			result = false;
		}

		return result;
	}

	private boolean isValidTopic(String article) {
		
		String[] splitArticle = article.split(DELIM_ARTICLE);
		if(!splitArticle[0].isEmpty() && !types.contains(splitArticle[0])) {
			return false;
		}
		
		return true;
	}

	public boolean unsubscribe(String ip, int port, String article) throws RemoteException {

		boolean result = true;
		String[] splitArticle;
		logger.info("Article unsubscribe request received from " + ip + ", " + port + " for article: " + article);
		if (!isValidSubscription(article)) {
			logger.info("Artticle invalid for unsubscription request");
			result = false;
		} else {
			splitArticle = getSplitArticle(article);
			result = removeFromSubscriberMap(splitArticle, ip);
		}
		logger.info("Article unsubscribe status: " + result);
		return result;
	}

	private boolean removeFromSubscriberMap(String[] splitArticle, String iP) {
		boolean result = true;

		if (splitArticle.length == 0) {
			result = false;
		} else {

			for (String topic : splitArticle) {

				if (!articleSubscriberMap.containsKey(topic)) {
					result = false;
					break;
				} else {
					Set<String> subscribers = articleSubscriberMap.get(topic);
					if (subscribers.contains(iP)) {
						subscribers.remove(iP);
						articleSubscriberMap.put(topic, subscribers);
					} else {
						result = false;
						break;
					}
				}
			}
		}
		return result;
	}

	public boolean publish(String ip, int port, String article) throws RemoteException {
		logger.info("Client with ip: " + ip + " and port: " + port + ", requesting to publish: " + article);
		boolean result = false;
		List<String> topics = new ArrayList<String>();
		Set<String> subscribers = new HashSet<String>();

		if (isValidArticleForPublish(article)) {
			logger.info("Article is valid to publish");
			topics = getTopicsToPublish(article);
			subscribers = getSubscribers(topics, ip);
			// Since we dont have to publish to clients who subscribe later
			System.out.println("subscribers size:" + subscribers.size());
			if (subscribers != null && subscribers.size() != 0) {
				result = sendToSubscribers(subscribers, article);
			} else {
				logger.info("No client subscribed to this article");
			}
		}
		return result;
	}

	private boolean sendToSubscribers(Set<String> subscribers, String article) {

		Map<String, String> clientArticleMap = new HashMap<String, String>();
		boolean result = false;
		logger.info("Sending published articles to subscribed clients");
		for (String client : subscribers) {
			clientArticleMap.put(client, article);
		}
		for (String client : clientArticleMap.keySet()) {
			int clientPort = clientIpPortMap.get(client);
			result = sendViaUDP(client, clientPort, article);

		}
		return result;
	}

	private boolean sendViaUDP(String clientIp, int clientPort, String article) {
		
		byte[] buf = new byte[1024];
		buf = article.getBytes();
		DatagramSocket socket;
		boolean result = false;
		try {
			socket = new DatagramSocket();
			InetAddress address = InetAddress.getByName(clientIp);
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, clientPort);
			socket.send(packet);
			logger.info("Article sent to subscribed client: " + clientIp + "," + clientPort + " via UDP");
			result = true;

		} catch (SocketException e) {
			logger.severe("Socket Exception while publishing article");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			logger.severe("Given client Host Unknown to publish article");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	private Set<String> getSubscribers(List<String> topics, String iP) {

		Set<String> subscribers = new HashSet<String>();

		for (String topic : topics) {
			Set<String> set;
			if (articleSubscriberMap.containsKey(topic)) {
				set = articleSubscriberMap.get(topic);
				subscribers.addAll(set);
			}
		}
		if (subscribers.contains(iP)) {
			subscribers.remove(iP);
		}
		return subscribers;
	}

	private List<String> getTopicsToPublish(String article) {
		// To remove contents
		String topicString = article.substring(0, article.lastIndexOf(";"));

		String[] topics = topicString.split(";");

		return Arrays.asList(topics);
	}

	private boolean isValidArticleForPublish(String article) {

		if (isLengthValid(article) && isValidTopic(article)) {

			String articleParts[] = article.split(DELIM_ARTICLE);
			return !articleParts[articleParts.length - 1].isEmpty(); // assuming blank space is also considered valid
																		// content
		} 
		return false;
	}

	private boolean isLengthValid(String article) {

		return article != null && article.length() != 0 && article.length() <= MAXSTRING;
	}

	public boolean ping() throws RemoteException {

		return true;
	}

}
