package com.symphony.bdk.examples;

import static com.symphony.bdk.core.config.BdkConfigLoader.loadFromSymphonyDir;

import com.symphony.bdk.core.SymphonyBdk;
import com.symphony.bdk.core.auth.BotAuthSession;
import com.symphony.bdk.core.auth.exception.AuthInitializationException;
import com.symphony.bdk.core.auth.exception.AuthUnauthorizedException;
import com.symphony.bdk.core.config.exception.BdkConfigException;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.StreamFilter;
import com.symphony.bdk.gen.api.model.V4Message;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * This very basic example demonstrates how send a message using both regular and OBO authentication modes.
 */
@Slf4j
public class AuthMain {

  private static final String STREAM = "2IFEMquh3pOHAxcgLF8jU3___ozwgwIVdA";
  private static final Message MESSAGE = Message.builder().content("<messageML>Hello, World</messageML>").build();

  public static void main(String[] args) throws BdkConfigException, AuthInitializationException, AuthUnauthorizedException {

    // setup SymphonyBdk facade object
    final SymphonyBdk bdk = new SymphonyBdk(loadFromSymphonyDir("config.yaml"));

    // send regular message using the Bot service account
    final V4Message regularMessage = bdk.messages().send(STREAM, MESSAGE);
    log.info("Regular message sent : {}", regularMessage.getMessageId());

    BotAuthSession oboSession = bdk.obo("user.name");

    bdk.obo(oboSession).streams().listStreams(new StreamFilter());
    bdk.obo(oboSession).messages().send(STREAM, MESSAGE);
    bdk.obo(oboSession).users().listUsersByUsernames(Arrays.asList("user.name"), true);
  }
}
