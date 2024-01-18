package com.symphony.bdk.ext.group.auth;

import static com.symphony.bdk.http.api.Pair.pair;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import com.symphony.bdk.core.auth.BotAuthSession;
import com.symphony.bdk.core.retry.RetryWithRecovery;
import com.symphony.bdk.core.retry.RetryWithRecoveryBuilder;
import com.symphony.bdk.http.api.ApiClient;
import com.symphony.bdk.http.api.ApiException;
import com.symphony.bdk.http.api.ApiResponse;
import com.symphony.bdk.http.api.util.TypeReference;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.apiguardian.api.API;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

@API(status = API.Status.INTERNAL)
public class OAuthSession {

  private final ApiClient loginClient;
  private final BotAuthSession session;
  private final RetryWithRecoveryBuilder<?> retryBuilder;

  @Getter
  private String bearerToken;

  public OAuthSession(
      @Nonnull ApiClient loginClient,
      @Nonnull BotAuthSession session,
      @Nonnull RetryWithRecoveryBuilder<?> retryBuilder
  ) {
    this.loginClient = loginClient;
    this.session = session;
    this.retryBuilder = RetryWithRecoveryBuilder.copyWithoutRecoveryStrategies(retryBuilder)
        .recoveryStrategy(ApiException::isUnauthorized, session::refresh);;
  }

  /**
   * Refreshes internal Bearer authentication token from bot's sessionToken.
   *
   * <p>Note that this method uses the retry strategy to refresh sessionToken if it has expired.
   */
  public void refresh() {
    this.bearerToken = RetryWithRecovery.executeAndRetry(
        this.retryBuilder,
        "groupExt.auth",
        this.loginClient.getBasePath(),
        this::doRefresh
    );
  }

  private String doRefresh() throws ApiException {
    Map<String, String> headers = new HashMap<>(4);
    headers.put("sessionToken", this.session.getSessionToken());

    final ApiResponse<TokenResponse> response = this.loginClient.invokeAPI(
        "/idm/tokens",
        "POST",
        singletonList(pair("scope", "profile-manager")),
        null,
        headers,
        null,
        null,
        "application/json",
        "application/json",
        null,
        new TypeReference<TokenResponse>() {}
    );

    return response.getData().getToken();
  }

  @Getter @Setter
  @API(status = API.Status.INTERNAL)
  public static class TokenResponse {

    @JsonProperty("access_token")
    private String token;
  }
}
