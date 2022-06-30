package com.symphony.bdk.bdd.mock;

import com.symphony.bdk.core.service.datafeed.DatafeedLoop;
import com.symphony.bdk.core.service.datafeed.RealTimeEventListener;
import com.symphony.bdk.gen.api.model.V4Initiator;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.gen.api.model.V4User;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MockDatafeedLoop implements BddMockService<DatafeedLoop> {

  private FakeDatafeedLoop mockDatafeedLoop;

  public MockDatafeedLoop() {
    this.mockDatafeedLoop = new FakeDatafeedLoop();
  }

  public void pushMessageToDatafeed(V4User initiator, V4Message message) {
    for (RealTimeEventListener listener : this.mockDatafeedLoop.listeners) {
      message.setMessage(FakeDatafeedLoop.START_TAG + message.getMessage() + "\n" + FakeDatafeedLoop.END_TAG);
      listener.onMessageSent(new V4Initiator().user(initiator), new V4MessageSent().message(message));
    }
  }

  public void submitElementsForm(V4User initiator, V4SymphonyElementsAction event) {
    for (RealTimeEventListener listener : this.mockDatafeedLoop.listeners) {
      listener.onSymphonyElementsAction(new V4Initiator().user(initiator), event);
    }
  }

  static class FakeDatafeedLoop implements DatafeedLoop {
    static final String START_TAG = "<div data-format=\"PresentationML\" data-version=\"2.0\">\n";
    static final String END_TAG = "</div>";
    final List<RealTimeEventListener> listeners = new ArrayList<>();

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void subscribe(RealTimeEventListener realTimeEventListener) {
      listeners.add(realTimeEventListener);
    }

    @Override
    public void unsubscribe(RealTimeEventListener realTimeEventListener) {
      listeners.remove(realTimeEventListener);
    }
  }


  @Override
  public DatafeedLoop mockedInstance() {
    return mockDatafeedLoop;
  }

  @Override
  public Class<DatafeedLoop> mockedClazz() {
    return DatafeedLoop.class;
  }
}
