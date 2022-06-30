package com.symphony.bdk.bdd.stepdefs;

import static com.symphony.bdk.bdd.assertion.MessageServiceAssert.assertThat;
import static com.symphony.bdk.core.activity.command.SlashCommand.slash;

import com.symphony.bdk.bdd.mock.MockDatafeedLoop;
import com.symphony.bdk.core.activity.ActivityRegistry;
import com.symphony.bdk.core.activity.parsing.Hashtag;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.bdk.gen.api.model.V4User;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;


public class SlasCommandActivityBddTest {

  @Autowired
  MessageService messageService;

  @Autowired
  MockDatafeedLoop datafeedLoop;

  @Autowired
  ActivityRegistry activityRegistry;

  @Given("slash hashtag command {string}")
  public void subscribeActivity(String commandPattern) {
    activityRegistry.register(slash(commandPattern, false, context ->
        {
          final Hashtag hashtag = context.getArguments().getHashtag("hashtag");
          messageService.send(context.getStreamId(), "Hashtag value: " + hashtag.getValue());
        }
    ));
  }

  @DataTableType
  public V4Message convert(Map<String, String> entry) {
    return new V4Message().message(entry.get("message"))
        .stream(new V4Stream().streamId(entry.get("streamId")).streamType(entry.get("streamType")))
        .user(new V4User().displayName(entry.get("displayName")).username(entry.get("username")))
        .data(entry.get("data"));
  }

  @When("received message sent event")
  public void received_message_sent_event(V4Message message) {
    this.datafeedLoop.pushMessageToDatafeed(message.getUser(), message);
  }

  @Then("a message back to symphony should be {string}")
  public void message_is_sent(String expected) {
    assertThat(messageService).hasSentMessage(expected);
  }
}
