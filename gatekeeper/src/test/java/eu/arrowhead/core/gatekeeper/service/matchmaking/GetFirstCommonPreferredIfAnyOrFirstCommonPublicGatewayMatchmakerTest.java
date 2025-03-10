package eu.arrowhead.core.gatekeeper.service.matchmaking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import eu.arrowhead.common.database.entity.Cloud;
import eu.arrowhead.common.database.entity.CloudGatekeeperRelay;
import eu.arrowhead.common.database.entity.CloudGatewayRelay;
import eu.arrowhead.common.database.entity.Relay;
import eu.arrowhead.common.dto.internal.RelayRequestDTO;
import eu.arrowhead.common.dto.internal.RelayType;
import eu.arrowhead.core.gatekeeper.database.service.GatekeeperDBService;

@RunWith(SpringRunner.class)
public class GetFirstCommonPreferredIfAnyOrFirstCommonPublicGatewayMatchmakerTest {

	//=================================================================================================
	// members
	
	@InjectMocks
	private final RelayMatchmakingAlgorithm algorithm = new GetFirstCommonPreferredIfAnyOrFirstCommonPublicGatewayMatchmaker();
	
	@Mock
	private GatekeeperDBService gatekeeperDBService;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingWithHavingCommonPreferredAndPublicGatewayRelays() {
		final Cloud localInstanceOfRequesterCloud = new Cloud();
		localInstanceOfRequesterCloud.setId(1);
		localInstanceOfRequesterCloud.setOperator("test-operator");
		localInstanceOfRequesterCloud.setName("test-name");
		localInstanceOfRequesterCloud.setSecure(true);
		localInstanceOfRequesterCloud.setNeighbor(true);
		localInstanceOfRequesterCloud.setOwnCloud(false);
		localInstanceOfRequesterCloud.setAuthenticationInfo("test-auth-info");
		
		final String commonRelayAddress1 = "test-address-1";
		final int commonRelayPort1 = 1000;		
		final String commonRelayAddress2 = "test-address-2";
		final int commonRelayPort2 = 2000;
		
		final Relay localInstanceOfPreferredRelay1 = new Relay();
		localInstanceOfPreferredRelay1.setId(3);
		localInstanceOfPreferredRelay1.setAddress(commonRelayAddress1);
		localInstanceOfPreferredRelay1.setPort(commonRelayPort1);
		localInstanceOfPreferredRelay1.setSecure(true);
		localInstanceOfPreferredRelay1.setExclusive(true);
		localInstanceOfPreferredRelay1.setType(RelayType.GATEWAY_RELAY);
		
		final RelayRequestDTO preferredRelay1 = new RelayRequestDTO();
		preferredRelay1.setAddress(commonRelayAddress1);
		preferredRelay1.setPort(commonRelayPort1);
		
		final Relay localInstanceOfPublicRelay2 = new Relay();
		localInstanceOfPublicRelay2.setId(4);
		localInstanceOfPublicRelay2.setAddress(commonRelayAddress2);
		localInstanceOfPublicRelay2.setPort(commonRelayPort2);
		localInstanceOfPublicRelay2.setSecure(true);
		localInstanceOfPublicRelay2.setExclusive(false);
		localInstanceOfPublicRelay2.setType(RelayType.GATEWAY_RELAY);
		
		final RelayRequestDTO preferredRelay2 = new RelayRequestDTO();
		preferredRelay2.setAddress(commonRelayAddress2);
		preferredRelay2.setPort(commonRelayPort2);
		
		final RelayMatchmakingParameters parameters = createRelayMatchmakingParametersWithLocalRelayConnsOfRequesterCloud(localInstanceOfRequesterCloud,
																														  List.of(localInstanceOfPreferredRelay1, localInstanceOfPublicRelay2),
																														  System.currentTimeMillis());
		parameters.setPreferredGatewayRelays(List.of(preferredRelay1, preferredRelay2));
		
		final Relay relayMatch = algorithm.doMatchmaking(parameters);
		
		assertEquals(localInstanceOfPreferredRelay1.getId(), relayMatch.getId());	
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingWithHavingCommonPublicGatewayAndGeneralRelays() {
		
		final Cloud localInstanceOfRequesterCloud = new Cloud();
		localInstanceOfRequesterCloud.setId(1);
		localInstanceOfRequesterCloud.setOperator("test-operator");
		localInstanceOfRequesterCloud.setName("test-name");
		localInstanceOfRequesterCloud.setSecure(true);
		localInstanceOfRequesterCloud.setNeighbor(true);
		localInstanceOfRequesterCloud.setOwnCloud(false);
		localInstanceOfRequesterCloud.setAuthenticationInfo("test-auth-info");
		
		final String commonRelayAddress1 = "test-address-1";
		final int commonRelayPort1 = 1000;		
		final String commonRelayAddress2 = "test-address-2";
		final int commonRelayPort2 = 2000;
		
		final Relay localInstanceOfKnownRelay1 = new Relay();
		localInstanceOfKnownRelay1.setId(3);
		localInstanceOfKnownRelay1.setAddress(commonRelayAddress1);
		localInstanceOfKnownRelay1.setPort(commonRelayPort1);
		localInstanceOfKnownRelay1.setSecure(true);
		localInstanceOfKnownRelay1.setExclusive(false);
		localInstanceOfKnownRelay1.setType(RelayType.GATEWAY_RELAY);
		
		final RelayRequestDTO knownRelay1 = new RelayRequestDTO();
		knownRelay1.setAddress(commonRelayAddress1);
		knownRelay1.setPort(commonRelayPort1);
		
		final Relay localInstanceOfKnownRelay2 = new Relay();
		localInstanceOfKnownRelay2.setId(4);
		localInstanceOfKnownRelay2.setAddress(commonRelayAddress2);
		localInstanceOfKnownRelay2.setPort(commonRelayPort2);
		localInstanceOfKnownRelay2.setSecure(true);
		localInstanceOfKnownRelay2.setExclusive(false);
		localInstanceOfKnownRelay2.setType(RelayType.GENERAL_RELAY);
		
		final RelayRequestDTO knownRelay2 = new RelayRequestDTO();
		knownRelay2.setAddress(commonRelayAddress2);
		knownRelay2.setPort(commonRelayPort2);
		
		final RelayMatchmakingParameters parameters = createRelayMatchmakingParametersWithLocalRelayConnsOfRequesterCloud(localInstanceOfRequesterCloud,
																														  List.of(),
																														  System.currentTimeMillis());
		parameters.setKnownGatewayRelays(List.of(knownRelay1, knownRelay2));		
		when(gatekeeperDBService.getPublicGatewayRelays()).thenReturn(List.of(localInstanceOfKnownRelay1, localInstanceOfKnownRelay2));
		
		final Relay relayMatch = algorithm.doMatchmaking(parameters);
		
		assertEquals(RelayType.GATEWAY_RELAY, relayMatch.getType());	
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingWithHavingMoreCommonPublicGatewayRelays() {
		final Cloud localInstanceOfRequesterCloud = new Cloud();
		localInstanceOfRequesterCloud.setId(1);
		localInstanceOfRequesterCloud.setOperator("test-operator");
		localInstanceOfRequesterCloud.setName("test-name");
		localInstanceOfRequesterCloud.setSecure(true);
		localInstanceOfRequesterCloud.setNeighbor(true);
		localInstanceOfRequesterCloud.setOwnCloud(false);
		localInstanceOfRequesterCloud.setAuthenticationInfo("test-auth-info");
		
		final String commonRelayAddress1 = "test-address-1";
		final int commonRelayPort1 = 1000;		
		final String commonRelayAddress2 = "test-address-2";
		final int commonRelayPort2 = 2000;
		
		final Relay localInstanceOfKnownRelay1 = new Relay();
		localInstanceOfKnownRelay1.setId(3);
		localInstanceOfKnownRelay1.setAddress(commonRelayAddress1);
		localInstanceOfKnownRelay1.setPort(commonRelayPort1);
		localInstanceOfKnownRelay1.setSecure(true);
		localInstanceOfKnownRelay1.setExclusive(false);
		localInstanceOfKnownRelay1.setType(RelayType.GATEWAY_RELAY);
		
		final RelayRequestDTO knownRelay1 = new RelayRequestDTO();
		knownRelay1.setAddress(commonRelayAddress1);
		knownRelay1.setPort(commonRelayPort1);
		
		final Relay localInstanceOfKnownRelay2 = new Relay();
		localInstanceOfKnownRelay2.setId(4);
		localInstanceOfKnownRelay2.setAddress(commonRelayAddress2);
		localInstanceOfKnownRelay2.setPort(commonRelayPort2);
		localInstanceOfKnownRelay2.setSecure(true);
		localInstanceOfKnownRelay2.setExclusive(false);
		localInstanceOfKnownRelay2.setType(RelayType.GATEWAY_RELAY);
		
		final RelayRequestDTO knownRelay2 = new RelayRequestDTO();
		knownRelay2.setAddress(commonRelayAddress2);
		knownRelay2.setPort(commonRelayPort2);
		
		final RelayMatchmakingParameters parameters = createRelayMatchmakingParametersWithLocalRelayConnsOfRequesterCloud(localInstanceOfRequesterCloud,
																														  List.of(),
																														  System.currentTimeMillis());
		parameters.setKnownGatewayRelays(List.of(knownRelay1, knownRelay2));		
		final List<Relay> publicGWRelays = List.of(localInstanceOfKnownRelay1, localInstanceOfKnownRelay2);
		when(gatekeeperDBService.getPublicGatewayRelays()).thenReturn(publicGWRelays);
		
		final Relay relayMatch = algorithm.doMatchmaking(parameters);
		
		assertEquals(publicGWRelays.get(0).getId(), relayMatch.getId());	
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingWithHavingMoreCommonPublicGeneralRelays() {
		final Cloud localInstanceOfRequesterCloud = new Cloud();
		localInstanceOfRequesterCloud.setId(1);
		localInstanceOfRequesterCloud.setOperator("test-operator");
		localInstanceOfRequesterCloud.setName("test-name");
		localInstanceOfRequesterCloud.setSecure(true);
		localInstanceOfRequesterCloud.setNeighbor(true);
		localInstanceOfRequesterCloud.setOwnCloud(false);
		localInstanceOfRequesterCloud.setAuthenticationInfo("test-auth-info");
		
		final String commonRelayAddress1 = "test-address-1";
		final int commonRelayPort1 = 1000;		
		final String commonRelayAddress2 = "test-address-2";
		final int commonRelayPort2 = 2000;
		
		final Relay localInstanceOfKnownRelay1 = new Relay();
		localInstanceOfKnownRelay1.setId(3);
		localInstanceOfKnownRelay1.setAddress(commonRelayAddress1);
		localInstanceOfKnownRelay1.setPort(commonRelayPort1);
		localInstanceOfKnownRelay1.setSecure(true);
		localInstanceOfKnownRelay1.setExclusive(false);
		localInstanceOfKnownRelay1.setType(RelayType.GENERAL_RELAY);
		
		final RelayRequestDTO knownRelay1 = new RelayRequestDTO();
		knownRelay1.setAddress(commonRelayAddress1);
		knownRelay1.setPort(commonRelayPort1);
		
		final Relay localInstanceOfKnownRelay2 = new Relay();
		localInstanceOfKnownRelay2.setId(4);
		localInstanceOfKnownRelay2.setAddress(commonRelayAddress2);
		localInstanceOfKnownRelay2.setPort(commonRelayPort2);
		localInstanceOfKnownRelay2.setSecure(true);
		localInstanceOfKnownRelay2.setExclusive(false);
		localInstanceOfKnownRelay2.setType(RelayType.GENERAL_RELAY);
		
		final RelayRequestDTO knownRelay2 = new RelayRequestDTO();
		knownRelay2.setAddress(commonRelayAddress2);
		knownRelay2.setPort(commonRelayPort2);
		
		final RelayMatchmakingParameters parameters = createRelayMatchmakingParametersWithLocalRelayConnsOfRequesterCloud(localInstanceOfRequesterCloud,
																														  List.of(),
																														  System.currentTimeMillis());
		parameters.setKnownGatewayRelays(List.of(knownRelay1, knownRelay2));		
		final List<Relay> publicGWRelays = List.of(localInstanceOfKnownRelay1, localInstanceOfKnownRelay2);
		when(gatekeeperDBService.getPublicGatewayRelays()).thenReturn(publicGWRelays);
		
		final Relay relayMatch = algorithm.doMatchmaking(parameters);
		
		assertEquals(publicGWRelays.get(0).getId(), relayMatch.getId());	
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingWithHavingNoCommonRelays() {
		final Cloud localInstanceOfRequesterCloud = new Cloud();
		localInstanceOfRequesterCloud.setId(1);
		localInstanceOfRequesterCloud.setOperator("test-operator");
		localInstanceOfRequesterCloud.setName("test-name");
		localInstanceOfRequesterCloud.setSecure(true);
		localInstanceOfRequesterCloud.setNeighbor(true);
		localInstanceOfRequesterCloud.setOwnCloud(false);
		localInstanceOfRequesterCloud.setAuthenticationInfo("test-auth-info");
		
		final Relay localInstanceOfKnownRelay1 = new Relay();
		localInstanceOfKnownRelay1.setId(3);
		localInstanceOfKnownRelay1.setAddress("test-address-1");
		localInstanceOfKnownRelay1.setPort(1000);
		localInstanceOfKnownRelay1.setSecure(true);
		localInstanceOfKnownRelay1.setExclusive(false);
		localInstanceOfKnownRelay1.setType(RelayType.GENERAL_RELAY);
		
		final RelayRequestDTO knownRelay1 = new RelayRequestDTO();
		knownRelay1.setAddress("test-address-2");
		knownRelay1.setPort(2000);
		
		final Relay localInstanceOfKnownRelay2 = new Relay();
		localInstanceOfKnownRelay2.setId(4);
		localInstanceOfKnownRelay2.setAddress("test-address-3");
		localInstanceOfKnownRelay2.setPort(3000);
		localInstanceOfKnownRelay2.setSecure(true);
		localInstanceOfKnownRelay2.setExclusive(false);
		localInstanceOfKnownRelay2.setType(RelayType.GENERAL_RELAY);
		
		final RelayRequestDTO knownRelay2 = new RelayRequestDTO();
		knownRelay2.setAddress("test-address-4");
		knownRelay2.setPort(4000);
		
		final RelayMatchmakingParameters parameters = createRelayMatchmakingParametersWithLocalRelayConnsOfRequesterCloud(localInstanceOfRequesterCloud,
																														  List.of(),
																														  System.currentTimeMillis());
		parameters.setKnownGatewayRelays(List.of(knownRelay1, knownRelay2));		
		final List<Relay> publicGWRelays = List.of(localInstanceOfKnownRelay1, localInstanceOfKnownRelay2);
		when(gatekeeperDBService.getPublicGatewayRelays()).thenReturn(publicGWRelays);
		
		final Relay relayMatch = algorithm.doMatchmaking(parameters);
		
		assertNull(relayMatch);	
	}
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private RelayMatchmakingParameters createRelayMatchmakingParametersWithLocalRelayConnsOfRequesterCloud(final Cloud cloud, final List<Relay> gatewayRelays, final long randomSeed) {
		for (final Relay relay : gatewayRelays) {
			if (relay.getType() == RelayType.GATEWAY_RELAY && relay.getExclusive()) {
				final CloudGatewayRelay conn = new CloudGatewayRelay(cloud, relay);
				cloud.getGatewayRelays().add(conn);
				relay.getCloudGateways().add(conn);
			} else {
				final CloudGatekeeperRelay conn = new CloudGatekeeperRelay(cloud, relay);
				cloud.getGatekeeperRelays().add(conn);
				relay.getCloudGatekeepers().add(conn);				
			}
		}
	
		final RelayMatchmakingParameters parameters = new RelayMatchmakingParameters(cloud);
		parameters.setRandomSeed(randomSeed);
		
		return parameters;
	}
}
