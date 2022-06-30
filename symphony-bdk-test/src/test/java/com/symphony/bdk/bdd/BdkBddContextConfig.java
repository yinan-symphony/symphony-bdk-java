package com.symphony.bdk.bdd;

import static org.mockito.Mockito.mock;

import com.symphony.bdk.bdd.mock.MockServicesFactory;
import com.symphony.bdk.core.activity.AbstractActivity;
import com.symphony.bdk.core.activity.ActivityRegistry;
import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.auth.AuthenticatorFactory;
import com.symphony.bdk.core.client.ApiClientFactory;
import com.symphony.bdk.core.service.application.ApplicationService;
import com.symphony.bdk.core.service.connection.ConnectionService;
import com.symphony.bdk.core.service.datafeed.DatafeedLoop;
import com.symphony.bdk.core.service.disclaimer.DisclaimerService;
import com.symphony.bdk.core.service.health.HealthService;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.presence.PresenceService;
import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.core.service.signal.SignalService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.core.service.user.UserService;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.template.api.TemplateEngine;
import com.symphony.bdk.template.freemarker.FreeMarkerEngine;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@CucumberContextConfiguration
@SpringBootTest(classes = BdkBddContextConfig.TestConfig.class)
public class BdkBddContextConfig {

  @TestConfiguration
  public static class TestConfig {
    MockServicesFactory mockServiceFactory;

    public TestConfig(@Autowired MockServicesFactory mockServicesFactory) {
      this.mockServiceFactory = mockServicesFactory;
    }

    @Bean
    public SessionService sessionService() {
      return mockServiceFactory.mockService(SessionService.class);
    }

    @Bean
    public StreamService streamService() {
      return mock(StreamService.class);
    }

    @Bean
    public UserService userService() {
      return mock(UserService.class);
    }

    @Bean
    public DisclaimerService disclaimerService() {
      return mock(DisclaimerService.class);
    }

    @Bean
    public PresenceService presenceService() {
      return mock(PresenceService.class);
    }

    @Bean
    public ConnectionService connectionService() {
      return mock(ConnectionService.class);
    }

    @Bean
    public SignalService signalService() {
      return mock(SignalService.class);
    }

    @Bean
    public ApplicationService applicationService() {
      return mock(ApplicationService.class);
    }

    @Bean
    public HealthService healthService() {
      return mock(HealthService.class);
    }

    @Bean
    public MessageService messageService() {
      return mock(MessageService.class);
    }

//    @Bean
//    public RealTimeEventsDispatcher realTimeEventsDispatcher(ApplicationEventPublisher publisher) {
//      return new RealTimeEventsDispatcher(publisher);
//    }

    @Bean
    public ApiClientFactory apiClientFactory() {
      return mock(ApiClientFactory.class);
    }

    @Bean
    public AuthenticatorFactory authenticatorFactory() {
      return mock(AuthenticatorFactory.class);
    }

    @Bean
    public TemplateEngine templateEngine() {
      return new FreeMarkerEngine();
    }

    @Bean
    public AuthSession botSession() {
      return mock(AuthSession.class);
    }

    @Bean
    public DatafeedLoop datafeedLoop() {
      return mockServiceFactory.mockService(DatafeedLoop.class);
    }

    @Bean
    public ActivityRegistry activityRegistry(
        final SessionService sessionService,
        final DatafeedLoop datafeedLoop,
        final List<AbstractActivity<?, ?>> activities
    ) {
      final UserV2 botSessionInfo = sessionService.getSession();
      final ActivityRegistry activityRegistry = new ActivityRegistry(botSessionInfo, datafeedLoop);
      activities.forEach(activityRegistry::register);
      return activityRegistry;
    }

//    @Bean
//    public static SlashAnnotationProcessor slashAnnotationProcessor() {
//      return new SlashAnnotationProcessor();
//    }
  }
}
