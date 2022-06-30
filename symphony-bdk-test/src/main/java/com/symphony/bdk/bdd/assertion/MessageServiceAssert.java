package com.symphony.bdk.bdd.assertion;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.core.service.message.MessageService;

import org.assertj.core.api.AbstractAssert;

public class MessageServiceAssert extends AbstractAssert<MessageServiceAssert, MessageService> {

  protected MessageServiceAssert(MessageService messageService) {
    super(messageService, MessageServiceAssert.class);
  }

  public static MessageServiceAssert assertThat(MessageService actual) {
    return new MessageServiceAssert(actual);
  }

  public MessageServiceAssert hasSentMessage(String message) {
    isNotNull();
//      form.put("message", message.getContent());
//      form.put("data", message.getData());
//      form.put("version", message.getVersion());
//      form.put("silent", message.getSilent());
//      form.put("attachment", toApiClientBodyParts(message.getAttachments()));
//      form.put("preview", toApiClientBodyParts(message.getPreviews()));
//    ArgumentCaptor<Map<String, Object>> capture = ArgumentCaptor.forClass(Map.class);
//    ApiResponse<V4Message> response = new ApiResponse<>(200, Collections.emptyMap(), Mockito.mock(V4Message.class));
//    doReturn(response).when(apiClient)
//        .invokeAPI(matches("^/v4/stream/./message/create$"), eq("POST"), anyList(), any(), anyMap(), anyMap(),
//            capture.capture(), eq("application/json"), eq("multipart/form-data"), any(), any(TypeReference.class));

    verify(actual).send(anyString(), eq(message));
    return this;
  }
}
