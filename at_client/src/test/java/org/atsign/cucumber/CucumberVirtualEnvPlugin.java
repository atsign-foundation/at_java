package org.atsign.cucumber;

import org.atsign.virtualenv.VirtualEnv;

import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestRunStarted;

public class CucumberVirtualEnvPlugin implements EventListener {

  @Override
  public void setEventPublisher(EventPublisher publisher) {
    publisher.registerHandlerFor(TestRunStarted.class, e -> VirtualEnv.setUp());
  }
}
