
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RMIClientConnectorTest {

	String clientIp = "10.131.82.78";
	int clientPort = 4567; // client's RMI port
	String regisIp = "134.84.182.32";

	RMIClientConnector rmiClientConnector = new RMIClientConnector(clientIp, clientPort, regisIp);

	

	@Test
	public void testJoin() {
		boolean response = rmiClientConnector.join();
		assertTrue(response);
	}

	@Test
	public void testPing() {

		boolean response = rmiClientConnector.ping();
		assertTrue(response);
	}

	@Test
	public void testSubscribe() {
		String article = "Science;;UMN;";
		boolean resp = rmiClientConnector.subscribe(article);
		assertTrue(resp);
	}

	@Test
	public void testPublishToSelf() {
		rmiClientConnector.join();
		String article = "Science;;UMN;";
		rmiClientConnector.subscribe(article);

		article = "Science;Someone;UMN;Contents";
		boolean response = rmiClientConnector.publish(article);
		assertFalse(response);
	}

	

	@Test
	public void testUnsubscribe() {
		String article = "Science;;UMN;";
		rmiClientConnector.join();
		rmiClientConnector.subscribe(article);

		boolean response = rmiClientConnector.unsubscribe(article);
		assertTrue(response);
	}

	
}
