package com.symphony.bdk.spring.config;

import com.symphony.bdk.core.auth.AuthenticatorFactory;
import com.symphony.bdk.core.auth.BotAuthSession;
import com.symphony.bdk.core.auth.CustomEnhancedAuthAuthenticator;
import com.symphony.bdk.core.auth.CustomEnhancedAuthSession;
import com.symphony.bdk.core.auth.ExtensionAppTokensRepository;
import com.symphony.bdk.core.auth.exception.AuthInitializationException;
import com.symphony.bdk.core.auth.exception.AuthUnauthorizedException;
import com.symphony.bdk.core.auth.impl.CustomEnhancedAuthAuthentication;
import com.symphony.bdk.core.auth.impl.EnhancedAuthSession;
import com.symphony.bdk.core.auth.impl.InMemoryTokensRepository;
import com.symphony.bdk.core.auth.impl.OAuthSession;
import com.symphony.bdk.core.auth.impl.OAuthentication;
import com.symphony.bdk.core.client.ApiClientFactory;
import com.symphony.bdk.core.config.model.BdkConfig;
import com.symphony.bdk.core.config.model.BdkCustomEnhancedAuthConfig;
import com.symphony.bdk.http.api.ApiClient;
import com.symphony.bdk.http.jersey2.ApiClientBuilderProviderJersey2;
import com.symphony.bdk.spring.SymphonyBdkCoreProperties;
import com.symphony.bdk.template.api.TemplateEngine;
import com.symphony.bdk.template.freemarker.FreeMarkerEngine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

import static com.symphony.bdk.core.auth.impl.OAuthentication.BEARER_AUTH;

/**
 * Configuration and injection of the main BDK/Core classes as beans within the Spring application context.
 */
@Slf4j
public class BdkCoreConfig {

  @Bean
  @ConditionalOnMissingBean
  public ApiClientFactory apiClientFactory(SymphonyBdkCoreProperties properties) {
    return new ApiClientFactory(properties,
        new ApiClientBuilderProviderJersey2()); // TODO create RestTemplate/or WebClient implementation
  }

  @Bean(name = "agentApiClient")
  public ApiClient agentApiClient(ApiClientFactory apiClientFactory) {
    return apiClientFactory.getAgentClient();
  }

  @Bean(name = "datafeedAgentApiClient")
  public ApiClient datafeedAgentApiClient(ApiClientFactory apiClientFactory) {
    return apiClientFactory.getDatafeedAgentClient();
  }

  @Bean(name = "datahoseAgentApiClient")
  public ApiClient datahoseAgentApiClient(ApiClientFactory apiClientFactory) {
    return apiClientFactory.getDatahoseAgentClient();
  }

  @Bean(name = "podApiClient")
//  @ConditionalOnMissingBean(CustomEnhancedAuthSession.class)
  public ApiClient podApiClient(ApiClientFactory apiClientFactory, Optional<BotAuthSession> botSession,
      Optional<CustomEnhancedAuthSession> optionalEnhancedAuthSession, BdkConfig config) {
    ApiClient client = apiClientFactory.getPodClient();
    if (config.isCommonJwtEnabled()) {
      if (config.isOboConfigured()) {
        throw new UnsupportedOperationException(
            "Common JWT feature is not available yet in OBO mode, please set commonJwt.enabled to false.");
      } else if (botSession.isPresent()) {
        final OAuthSession oAuthSession = new OAuthSession(botSession.get());
        client.getAuthentications().put(BEARER_AUTH, new OAuthentication(oAuthSession::getBearerToken));
        client.addEnforcedAuthenticationScheme(BEARER_AUTH);
      }
    }
    if (config.isEnhancedAuthEnabled()) {
      final CustomEnhancedAuthSession enhancedAuthSession = optionalEnhancedAuthSession.get();
      BdkCustomEnhancedAuthConfig enhancedAuthConfig = config.getEnhancedAuth();
      if (config.isCommonJwtEnabled() && "Authorization".equals(enhancedAuthConfig.getHeaderName())) {
        throw new UnsupportedOperationException(
            "Common JWT feature is enabled, it use 'Authorization' header too, there is a conflict with the enhanced authentication.");
      }
      client.getAuthentications()
          .put(enhancedAuthConfig.getId(), new CustomEnhancedAuthAuthentication(enhancedAuthConfig.getHeaderName(),
              enhancedAuthSession::getEnhancedAuthToken));
      client.addEnforcedAuthenticationScheme(enhancedAuthConfig.getId());
    }
    return client;
  }

  @Bean(name = "relayApiClient")
  public ApiClient relayApiClient(ApiClientFactory apiClientFactory) {
    return apiClientFactory.getRelayClient();
  }

  @Bean(name = "loginApiClient")
  public ApiClient loginApiClient(ApiClientFactory apiClientFactory) {
    return apiClientFactory.getLoginClient();
  }

  @Bean(name = "keyAuthApiClient")
  @ConditionalOnProperty("bdk.bot.certificatePath")
  public ApiClient keyAuthApiClient(ApiClientFactory apiClientFactory) {
    return apiClientFactory.getKeyAuthClient();
  }

  @Bean(name = "sessionAuthApiClient")
  @ConditionalOnProperty("bdk.bot.certificatePath")
  public ApiClient sessionAuthApiClient(ApiClientFactory apiClientFactory) {
    return apiClientFactory.getSessionAuthClient();
  }

  @Bean
  @ConditionalOnMissingBean
  public ExtensionAppTokensRepository extensionAppTokensRepository() {
    return new InMemoryTokensRepository();
  }

  @Bean
  @ConditionalOnMissingBean
  public AuthenticatorFactory authenticatorFactory(SymphonyBdkCoreProperties properties,
      ApiClientFactory apiClientFactory, ExtensionAppTokensRepository extensionAppTokensRepository) {
    return new AuthenticatorFactory(properties, apiClientFactory, extensionAppTokensRepository);
  }

  @Bean
  @ConditionalOnMissingBean
  public TemplateEngine templateEngine() {
    return new FreeMarkerEngine();
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty("bdk.bot.username")
  public BotAuthSession botSession(AuthenticatorFactory authenticatorFactory) {
    try {
      return authenticatorFactory.getBotAuthenticator().authenticateBot();
    } catch (AuthUnauthorizedException | AuthInitializationException e) {
      throw new BeanInitializationException("Unable to authenticate bot", e);
    }
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(value = "bdk.enhanced-auth.enabled", havingValue = "true")
  public CustomEnhancedAuthSession enhancedAuthSession(CustomEnhancedAuthAuthenticator enhancedAuthAuthenticator) {
    CustomEnhancedAuthSession enhancedAuthSession = new EnhancedAuthSession((enhancedAuthAuthenticator));
    try {
      enhancedAuthSession.refresh();
      return enhancedAuthSession;
    } catch (AuthUnauthorizedException e) {
      throw new BeanInitializationException("Unable to get enhanced authentication token.", e);
    }
  }

}
