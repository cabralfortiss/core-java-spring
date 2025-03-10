package eu.arrowhead.core.gateway.thread;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.BytesMessage;
import javax.jms.CompletionListener;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.dto.internal.GatewayProviderConnectionRequestDTO;
import eu.arrowhead.common.dto.internal.RelayRequestDTO;
import eu.arrowhead.common.dto.internal.RelayType;
import eu.arrowhead.common.dto.shared.CloudRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.core.gateway.relay.GatewayRelayClient;

@RunWith(SpringRunner.class)
public class ProviderSideSocketThreadTest {
	
	//=================================================================================================
	// members
	
	private ApplicationContext appContext;
	private GatewayRelayClient relayClient;
	
	private ProviderSideSocketThread testingObject;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Before
	public void setUp() {
		relayClient = mock(GatewayRelayClient.class, "relayClient");
		appContext = mock(ApplicationContext.class, "appContext");
		
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		when(appContext.getBean(CoreCommonConstants.GATEWAY_ACTIVE_SESSION_MAP, ConcurrentHashMap.class)).thenReturn(new ConcurrentHashMap<>());
		when(appContext.getBean(SSLProperties.class)).thenReturn(getTestSSLPropertiesForThread());
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		
		testingObject = new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 60000);
	}

	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorAppContextNull() {
		new ProviderSideSocketThread(null, null, null, null, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorRelayClientNull() {
		new ProviderSideSocketThread(appContext, null, null, null, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorRelaySessionNull() {
		new ProviderSideSocketThread(appContext, relayClient, null, null, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorRelaySessionClosed() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(true);
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), null, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestNull() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), null, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestProviderNull() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.setProvider(null);
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestProviderNameNull() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.getProvider().setSystemName(null);
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestProviderNameEmpty() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.getProvider().setSystemName(" ");
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestProviderAddressNull() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.getProvider().setAddress(null);
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestProviderAddressEmpty() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.getProvider().setAddress("\r\n");
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestProviderPortNull() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.getProvider().setPort(null);
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestProviderPortTooLow() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.getProvider().setPort(-2);
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestProviderPortTooHigh() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.getProvider().setPort(1111111);
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestProviderAuthenticationInfoNull() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.getProvider().setAuthenticationInfo(null);
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestProviderAuthenticationInfoEmpty() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.getProvider().setAuthenticationInfo("");
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestServiceDefinitionNull() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.setServiceDefinition(null);
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestServiceDefinitionEmpty() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.setServiceDefinition("\t\t\t");
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestConsumerGWPublicKeyNull() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.setConsumerGWPublicKey(null);
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorConnectionRequestConsumerGWPublicKeyEmpty() {
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		connectionRequest.setConsumerGWPublicKey(" ");
		
		new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 0);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConstructorOk() {
		Assert.assertEquals("provider.test-service", testingObject.getName());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testInitQueueIdNull() {
		testingObject.init(null, null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testInitQueueIdEmpty() {
		testingObject.init(" ", null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testInitSenderNull() {
		testingObject.init("queueId", null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitOk() {
		Assert.assertTrue(!testingObject.isInitialized());
		testingObject.init("queueId", getTestMessageProducer());
		Assert.assertTrue(testingObject.isInitialized());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testOnMessageOutProviderNull() {
		testingObject.onMessage(null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnMessageCloseControlMessage() throws JMSException {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(10);
		ReflectionTestUtils.setField(testingObject, "outProvider", outputStream);
		final ActiveMQTextMessage message = new ActiveMQTextMessage();
		message.setJMSDestination(new ActiveMQQueue("bla" + GatewayRelayClient.CONTROL_QUEUE_SUFFIX));
		doNothing().when(relayClient).handleCloseControlMessage(any(Message.class), any(Session.class));
		testingObject.onMessage(message);
		final boolean interrupted = (boolean) ReflectionTestUtils.getField(testingObject, "interrupted");
		Assert.assertTrue(interrupted);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnMessageExceptionThrown() throws JMSException {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(10);
		ReflectionTestUtils.setField(testingObject, "outProvider", outputStream);
		final ActiveMQTextMessage message = new ActiveMQTextMessage();
		message.setJMSDestination(new ActiveMQQueue("bla" + GatewayRelayClient.CONTROL_QUEUE_SUFFIX));
		doThrow(new JMSException("test")).when(relayClient).handleCloseControlMessage(any(Message.class), any(Session.class));
		testingObject.onMessage(message);
		final boolean interrupted = (boolean) ReflectionTestUtils.getField(testingObject, "interrupted");
		Assert.assertTrue(interrupted);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnMessageNormalMessage() throws JMSException {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(10);
		ReflectionTestUtils.setField(testingObject, "outProvider", outputStream);
		final ActiveMQTextMessage message = new ActiveMQTextMessage();
		message.setJMSDestination(new ActiveMQQueue("bla"));
		when(relayClient.getBytesFromMessage(any(Message.class), any(PublicKey.class))).thenReturn(new byte[] { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 });
		testingObject.onMessage(message);
		final boolean interrupted = (boolean) ReflectionTestUtils.getField(testingObject, "interrupted");
		Assert.assertTrue(!interrupted);
		Assert.assertArrayEquals(new byte[] { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 }, outputStream.toByteArray());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalStateException.class)
	public void testRunNotInitialized() {
		testingObject.run();
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWhenInternalExceptionThrown() {
		final SSLProperties sslProps = getTestSSLPropertiesForThread();
		ReflectionTestUtils.setField(sslProps, "keyStoreType", "invalid");
		when(appContext.getBean(SSLProperties.class)).thenReturn(sslProps);
		final GatewayProviderConnectionRequestDTO connectionRequest = getTestGatewayProviderConnectionRequestDTO();
		
		final ProviderSideSocketThread thread = new ProviderSideSocketThread(appContext, relayClient, getTestSession(), connectionRequest, 60000);
		thread.init("queueId", getTestMessageProducer());
		thread.run();

		final boolean interrupted = (boolean) ReflectionTestUtils.getField(thread, "interrupted");
		Assert.assertTrue(interrupted);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Ignore
	@Test
	public void testRunWhenOtherSideCloseTheConnectionAfterSendingSomeBytes() throws JMSException {
		doNothing().when(relayClient).sendBytes(any(Session.class), any(MessageProducer.class), any(PublicKey.class), any(byte[].class));
		testingObject.init("queueId", getTestMessageProducer());
		new Thread() {
			public void run() {
				try {
					final SSLContext sslContext = SSLContextFactory.createGatewaySSLContext(getTestSSLPropertiesForTestServerThread());
					final SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
					final SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(22002);
					sslServerSocket.setNeedClientAuth(true);
					sslServerSocket.setSoTimeout(600000);
					
					final SSLSocket sslConsumerSocket = (SSLSocket) sslServerSocket.accept();
					final OutputStream outConsumer = sslConsumerSocket.getOutputStream();
					outConsumer.write(new byte[] { 1, 2, 3, 4 });
					
					sslConsumerSocket.close();
					sslServerSocket.close();
				} catch (final Exception ex) {
					ex.printStackTrace();
				}
			};
		}.start();
		
		testingObject.run();
		verify(relayClient).sendBytes(any(Session.class), any(MessageProducer.class), any(PublicKey.class), any(byte[].class));
		final boolean interrupted = (boolean) ReflectionTestUtils.getField(testingObject, "interrupted");
		Assert.assertTrue(interrupted);
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private Session getTestSession() {
		return new Session() {

			//-------------------------------------------------------------------------------------------------
			public void close() throws JMSException {}
			public Queue createQueue(final String queueName) throws JMSException { return null;	}
			public Topic createTopic(final String topicName) throws JMSException { return null;	}
			public MessageConsumer createConsumer(final Destination destination) throws JMSException { return null; }
			public MessageProducer createProducer(final Destination destination) throws JMSException { return null;	}
			public TextMessage createTextMessage(final String text) throws JMSException { return null; }
			public BytesMessage createBytesMessage() throws JMSException { return null; }
			public MapMessage createMapMessage() throws JMSException { return null; }
			public Message createMessage() throws JMSException { return null; }
			public ObjectMessage createObjectMessage() throws JMSException { return null; }
			public ObjectMessage createObjectMessage(final Serializable object) throws JMSException { return null; }
			public StreamMessage createStreamMessage() throws JMSException { return null; }
			public TextMessage createTextMessage() throws JMSException { return null; }
			public boolean getTransacted() throws JMSException { return false; 	}
			public int getAcknowledgeMode() throws JMSException { return 0; }
			public void commit() throws JMSException {}
			public void rollback() throws JMSException {}
			public void recover() throws JMSException {}
			public MessageListener getMessageListener() throws JMSException { return null; }
			public void setMessageListener(final MessageListener listener) throws JMSException {}
			public void run() {}
			public MessageConsumer createConsumer(final Destination destination, final String messageSelector) throws JMSException { return null; }
			public MessageConsumer createConsumer(final Destination destination, final String messageSelector, final boolean noLocal) throws JMSException { return null; }
			public MessageConsumer createSharedConsumer(final Topic topic, final String sharedSubscriptionName) throws JMSException { return null; }
			public MessageConsumer createSharedConsumer(final Topic topic, final String sharedSubscriptionName, final String messageSelector) throws JMSException { return null; }
			public TopicSubscriber createDurableSubscriber(final Topic topic, final String name) throws JMSException { return null; }
			public TopicSubscriber createDurableSubscriber(final Topic topic, final String name, final String messageSelector, final boolean noLocal) throws JMSException { return null; }
			public MessageConsumer createDurableConsumer(final Topic topic, final String name) throws JMSException { return null; }
			public MessageConsumer createDurableConsumer(final Topic topic, final String name, final String messageSelector, final boolean noLocal) throws JMSException { return null; }
			public MessageConsumer createSharedDurableConsumer(final Topic topic, final String name) throws JMSException { return null; }
			public MessageConsumer createSharedDurableConsumer(final Topic topic, final String name, final String messageSelector) throws JMSException { return null;	}
			public QueueBrowser createBrowser(final Queue queue) throws JMSException { return null; }
			public QueueBrowser createBrowser(final Queue queue, final String messageSelector) throws JMSException { return null; }
			public TemporaryQueue createTemporaryQueue() throws JMSException { return null; }
			public TemporaryTopic createTemporaryTopic() throws JMSException { return null;	}
			public void unsubscribe(final String name) throws JMSException {}
		};
	}
	
	//-------------------------------------------------------------------------------------------------
	private GatewayProviderConnectionRequestDTO getTestGatewayProviderConnectionRequestDTO() {
		final RelayRequestDTO relay = new RelayRequestDTO("localhost", 1234, false, false, RelayType.GATEWAY_RELAY.name());
		final SystemRequestDTO consumer = new SystemRequestDTO();
		consumer.setSystemName("consumer");
		consumer.setAddress("abc.de");
		consumer.setPort(22001);
		consumer.setAuthenticationInfo("consAuth");
		final SystemRequestDTO provider = new SystemRequestDTO();
		provider.setSystemName("provider");
		provider.setAddress("127.0.0.1");
		provider.setPort(22002);
		provider.setAuthenticationInfo("provAuth");
		final CloudRequestDTO consumerCloud = new CloudRequestDTO();
		consumerCloud.setName("testcloud1");
		consumerCloud.setOperator("aitia");
		final CloudRequestDTO providerCloud = new CloudRequestDTO();
		providerCloud.setName("testcloud2");
		providerCloud.setOperator("elte");
		
		final String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq5Jq4tOeFoLqxOqtYcujbCNZina3iuV9+/o8D1R9D0HvgnmlgPlqWwjDSxV7m7SGJpuc/rRXJ85OzqV3rwRHO8A8YWXiabj8EdgEIyqg4SOgTN7oZ7MQUisTpwtWn9K14se4dHt/YE9mUW4en19p/yPUDwdw3ECMJHamy/O+Mh6rbw6AFhYvz6F5rXYB8svkenOuG8TSBFlRkcjdfqQqtl4xlHgmlDNWpHsQ3eFAO72mKQjm2ZhWI1H9CLrJf1NQs2GnKXgHBOM5ET61fEHWN8axGGoSKfvTed5vhhX7l5uwxM+AKQipLNNKjEaQYnyX3TL9zL8I7y+QkhzDa7/5kQIDAQAB";
		
		return new GatewayProviderConnectionRequestDTO(relay, consumer, provider, consumerCloud, providerCloud, "test-service", publicKey);
	}
	
	//-------------------------------------------------------------------------------------------------
	private MessageProducer getTestMessageProducer() {
		return new MessageProducer() {
			
			//-------------------------------------------------------------------------------------------------
			public void setTimeToLive(long timeToLive) throws JMSException {}
			public void setPriority(int defaultPriority) throws JMSException {}
			public void setDisableMessageTimestamp(boolean value) throws JMSException {}
			public void setDisableMessageID(boolean value) throws JMSException {}
			public void setDeliveryMode(int deliveryMode) throws JMSException {	}
			public void setDeliveryDelay(long deliveryDelay) throws JMSException {}
			public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive, CompletionListener completionListener) throws JMSException {}
			public void send(Message message, int deliveryMode, int priority, long timeToLive, CompletionListener completionListener) throws JMSException {}
			public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {}
			public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {}
			public void send(Destination destination, Message message, CompletionListener completionListener) throws JMSException {}
			public void send(Message message, CompletionListener completionListener) throws JMSException {}
			public void send(Destination destination, Message message) throws JMSException {}
			public void send(Message message) throws JMSException {}
			public long getTimeToLive() throws JMSException { return 0; }
			public int getPriority() throws JMSException { return 0; }
			public boolean getDisableMessageTimestamp() throws JMSException { return false;	}
			public boolean getDisableMessageID() throws JMSException { return false; }
			public Destination getDestination() throws JMSException { return null; }
			public int getDeliveryMode() throws JMSException { return 0; }
			public long getDeliveryDelay() throws JMSException { return 0; }
			public void close() throws JMSException {}
		};
	}
	
	//-------------------------------------------------------------------------------------------------
	private SSLProperties getTestSSLPropertiesForThread() {
		final SSLProperties sslProps = new SSLProperties();
		ReflectionTestUtils.setField(sslProps, "sslEnabled", true);
		ReflectionTestUtils.setField(sslProps, "keyStoreType", "PKCS12");
		final Resource keystore = new ClassPathResource("certificates/gateway.p12");
		ReflectionTestUtils.setField(sslProps, "keyStore", keystore);
		ReflectionTestUtils.setField(sslProps, "keyStorePassword", "123456");
		final Resource truststore = new ClassPathResource("certificates/truststore.p12");
		ReflectionTestUtils.setField(sslProps, "trustStore", truststore);
		ReflectionTestUtils.setField(sslProps, "trustStorePassword", "123456");
		
		return sslProps;
	}
	
	//-------------------------------------------------------------------------------------------------
	private SSLProperties getTestSSLPropertiesForTestServerThread() {
		final SSLProperties sslProps = new SSLProperties();
		ReflectionTestUtils.setField(sslProps, "sslEnabled", true);
		ReflectionTestUtils.setField(sslProps, "keyStoreType", "PKCS12");
		final Resource keystore = new ClassPathResource("certificates/authorization.p12");
		ReflectionTestUtils.setField(sslProps, "keyStore", keystore);
		ReflectionTestUtils.setField(sslProps, "keyStorePassword", "123456");
		final Resource truststore = new ClassPathResource("certificates/truststore.p12");
		ReflectionTestUtils.setField(sslProps, "trustStore", truststore);
		ReflectionTestUtils.setField(sslProps, "trustStorePassword", "123456");
		
		return sslProps;
	}
}