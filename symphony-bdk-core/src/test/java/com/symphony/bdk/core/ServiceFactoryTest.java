package com.symphony.bdk.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.auth.BotAuthSession;
import com.symphony.bdk.core.client.ApiClientFactory;
import com.symphony.bdk.core.config.BdkConfigLoader;
import com.symphony.bdk.core.config.exception.BdkConfigException;
import com.symphony.bdk.core.config.model.BdkBotConfig;
import com.symphony.bdk.core.config.model.BdkCommonJwtConfig;
import com.symphony.bdk.core.config.model.BdkConfig;
import com.symphony.bdk.core.config.model.BdkDatafeedConfig;
import com.symphony.bdk.core.config.model.BdkExtAppConfig;
import com.symphony.bdk.core.service.application.ApplicationService;
import com.symphony.bdk.core.service.connection.ConnectionService;
import com.symphony.bdk.core.service.datafeed.DatafeedLoop;
import com.symphony.bdk.core.service.datafeed.impl.DatafeedLoopV1;
import com.symphony.bdk.core.service.datafeed.impl.DatafeedLoopV2;
import com.symphony.bdk.core.service.health.HealthService;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.presence.PresenceService;
import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.core.service.signal.SignalService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.core.service.user.UserService;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.http.api.ApiClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServiceFactoryTest {

  private ServiceFactory serviceFactory;
  private ApiClientFactory apiClientFactory;
  private BotAuthSession mAuthSession;
  private ApiClient mPodClient;
  private BdkConfig config;
  private UserV2 botInfo;

  @BeforeEach
  void setUp() throws BdkConfigException {
    this.config = BdkConfigLoader.loadFromClasspath("/config/config.yaml");
    this.botInfo = mock(UserV2.class);
    this.mAuthSession = mock(BotAuthSession.class);
    this.mPodClient = mock(ApiClient.class);
    this.apiClientFactory = mock(ApiClientFactory.class);

    when(this.apiClientFactory.getPodClient()).thenReturn(mPodClient);
    when(this.apiClientFactory.getAgentClient()).thenReturn(mock(ApiClient.class));

    ApiClient datafeedAgentClient = mock(ApiClient.class);
    when(datafeedAgentClient.getBasePath()).thenReturn("/agent");
    when(this.apiClientFactory.getDatafeedAgentClient()).thenReturn(datafeedAgentClient);
    when(this.apiClientFactory.getDatahoseAgentClient()).thenReturn(datafeedAgentClient);

    this.serviceFactory = new ServiceFactory(this.apiClientFactory, this.mAuthSession, this.config);
  }

  @Test
  void getUserServiceTest() {
    UserService userService = this.serviceFactory.getUserService();
    assertNotNull(userService);
  }

  @Test
  void getStreamServiceTest() {
    StreamService streamService = this.serviceFactory.getStreamService();
    assertNotNull(streamService);
  }

  @Test
  void getPresenceServiceTest() {
    PresenceService presenceService = this.serviceFactory.getPresenceService();
    assertNotNull(presenceService);
  }

  @Test
  void getConnectionServiceTest() {
    ConnectionService connectionService = this.serviceFactory.getConnectionService();
    assertNotNull(connectionService);
  }

  @Test
  void getSignalServiceTest() {
    SignalService signalService = this.serviceFactory.getSignalService();
    assertNotNull(signalService);
  }

  @Test
  void getApplicationManagementServiceTest() {
    ApplicationService applicationManagementService = this.serviceFactory.getApplicationService();
    assertNotNull(applicationManagementService);
  }

  @Test
  void getSessionServiceTest() {
    SessionService sessionService = this.serviceFactory.getSessionService();
    assertNotNull(sessionService);
  }

  @Test
  void getMessageServiceTest() {
    MessageService messageService = this.serviceFactory.getMessageService();
    assertNotNull(messageService);
  }

  @Test
  void getHealthServiceTest() {
    HealthService healthService = this.serviceFactory.getHealthService();
    assertNotNull(healthService);
  }

  @Test
  void getDatafeedServiceTest() {
    BdkDatafeedConfig datafeedConfig = this.config.getDatafeed();
    datafeedConfig.setVersion("v1");

    this.serviceFactory = new ServiceFactory(this.apiClientFactory, mAuthSession, config);
    DatafeedLoop datafeedServiceV1 = this.serviceFactory.getDatafeedLoop(botInfo);
    assertNotNull(datafeedServiceV1);
    assertEquals(datafeedServiceV1.getClass(), DatafeedLoopV1.class);

    datafeedConfig.setVersion("v2");
    this.serviceFactory = new ServiceFactory(this.apiClientFactory, mAuthSession, config);
    DatafeedLoop datafeedServiceV2 = this.serviceFactory.getDatafeedLoop(botInfo);
    assertNotNull(datafeedServiceV2);
    assertEquals(datafeedServiceV2.getClass(), DatafeedLoopV2.class);
  }

  @Test
  void getDatahoseServiceTest() {
    assertNotNull(this.serviceFactory.getDatahoseLoop(this.botInfo));
  }

  @Test
  void testPodApiClientConfigWithCommonJwt() {
    BdkCommonJwtConfig bdkCommonJwtConfig = this.config.getCommonJwt();
    bdkCommonJwtConfig.setEnabled(true);
    config.setApp(new BdkExtAppConfig());

    this.serviceFactory = new ServiceFactory(this.apiClientFactory, mAuthSession, config);

    assertFalse(config.isOboConfigured());
    assertTrue(config.isBotConfigured());
    assertTrue(config.isCommonJwtEnabled());
    verify(mPodClient).getAuthentications();
    verify(mPodClient).addEnforcedAuthenticationScheme(eq("bearerAuth"));
  }

  @Test
  void testPodApiClientConfigWithCommonJwtInOboMode() {
    BdkCommonJwtConfig bdkCommonJwtConfig = this.config.getCommonJwt();
    bdkCommonJwtConfig.setEnabled(true);

    assertTrue(config.isOboConfigured());
    assertTrue(config.isBotConfigured());
    assertTrue(config.isCommonJwtEnabled());
    assertThrows(UnsupportedOperationException.class, ()-> new ServiceFactory(this.apiClientFactory, mAuthSession, config));
  }

  @Test
  void testPodApiClientConfigWithCommonJwtInOboOnlyMode() {
    BdkCommonJwtConfig bdkCommonJwtConfig = this.config.getCommonJwt();
    bdkCommonJwtConfig.setEnabled(true);
    config.setBot(new BdkBotConfig());

    assertTrue(config.isOboConfigured());
    assertFalse(config.isBotConfigured());
    assertTrue(config.isCommonJwtEnabled());
    assertThrows(UnsupportedOperationException.class, ()-> new ServiceFactory(this.apiClientFactory, mAuthSession, config));
  }
}
