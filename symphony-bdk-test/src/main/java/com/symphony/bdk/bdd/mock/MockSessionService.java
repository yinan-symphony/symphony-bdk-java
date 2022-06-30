package com.symphony.bdk.bdd.mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.gen.api.model.UserV2;

import org.springframework.stereotype.Component;

@Component
public class MockSessionService implements BddMockService<SessionService> {

  private final SessionService sessionService = mock(SessionService.class);

  public MockSessionService() {
    init();
  }

  private void init() {
    when(sessionService.getSession()).thenReturn(new UserV2().id(12345678L).accountType(
        UserV2.AccountTypeEnum.SYSTEM).displayName("BddBOT").username("bdd_bot"));
  }

  @Override
  public SessionService mockedInstance() {
    return sessionService;
  }

  @Override
  public Class<SessionService> mockedClazz() {
    return SessionService.class;
  }
}
