package eu.arrowhead.core.gatekeeper.relay;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.util.Assert;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.internal.GeneralAdvertisementMessageDTO;

public class GatekeeperRelayClientUsingCachedSessions implements GatekeeperRelayClient {
	
	//=================================================================================================
	// members
	
	private static final ConcurrentMap<String,Session> sessionCache = new ConcurrentHashMap<>();
	
	private final GatekeeperRelayClient client;
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public GatekeeperRelayClientUsingCachedSessions(final String serverCommonName, final PublicKey publicKey, final PrivateKey privateKey, final long timeout) {
		Assert.isTrue(!Utilities.isEmpty(serverCommonName), "Common name is null or blank.");
		Assert.notNull(publicKey, "Public key is null.");
		Assert.notNull(privateKey, "Private key is null.");
		
		this.client = GatekeeperRelayClientFactory.createGatekeeperRelayClient(serverCommonName, publicKey, privateKey, timeout, false);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public Session createConnection(final String host, final int port) throws JMSException {
		Assert.isTrue(!Utilities.isEmpty(host), "Host is null or blank.");
		Assert.isTrue(port > CommonConstants.SYSTEM_PORT_RANGE_MIN && port < CommonConstants.SYSTEM_PORT_RANGE_MAX, "Port is invalid.");
		
		synchronized (sessionCache) {
			Session session = createSessionIfNecessary(host, port);
			if (isConnectionClosed(session)) {
				sessionCache.values().remove(session);
				session = createSessionIfNecessary(host, port);
			}
			
			return session;
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void closeConnection(final Session session) {
		if (session != null) {
			synchronized (sessionCache) {
				sessionCache.values().remove(session);
			}
			client.closeConnection(session);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean isConnectionClosed(final Session session) {
		return client.isConnectionClosed(session);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public MessageConsumer subscribeGeneralAdvertisementTopic(final Session session) throws JMSException {
		return client.subscribeGeneralAdvertisementTopic(session);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public GeneralAdvertisementMessageDTO getGeneralAdvertisementMessage(final Message msg) throws JMSException {
		return client.getGeneralAdvertisementMessage(msg);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public GatekeeperRelayRequest sendAcknowledgementAndReturnRequest(final Session session, final GeneralAdvertisementMessageDTO gaMsg) throws JMSException {
		return client.sendAcknowledgementAndReturnRequest(session, gaMsg);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void sendResponse(final Session session, final GatekeeperRelayRequest request, final Object responsePayload) throws JMSException {
		client.sendResponse(session, request, responsePayload);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public GeneralAdvertisementResult publishGeneralAdvertisement(final Session session, final String recipientCN, final String recipientPublicKey) throws JMSException {
		return client.publishGeneralAdvertisement(session, recipientCN, recipientPublicKey);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public GatekeeperRelayResponse sendRequestAndReturnResponse(final Session session, final GeneralAdvertisementResult advResponse, final Object requestPayload) throws JMSException {
		return client.sendRequestAndReturnResponse(session, advResponse, requestPayload);
	}
	
	//-------------------------------------------------------------------------------------------------
	public List<Session> getCachedSessions() {
		return new ArrayList<>(sessionCache.values());
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private Session createSessionIfNecessary(final String host, final int port) throws JMSException {
		final String cacheKey = host + ":" + port;
		Session session = sessionCache.get(cacheKey);
		if (session == null) {
			session = client.createConnection(host, port);
			sessionCache.put(cacheKey, session);
		}
		
		return session;
	}
}