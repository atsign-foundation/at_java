package org.atsign.cucumber;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.atsign.cucumber.helpers.Helpers;
import org.atsign.virtualenv.VirtualEnv;

import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestRunStarted;

public class CucumberVirtualEnvPlugin implements EventListener {

  @Override
  public void setEventPublisher(EventPublisher publisher) {
    if (!Helpers.isHostPortReachable("vip.ve.atsign.zone:64", SECONDS.toMillis(2))) {
      publisher.registerHandlerFor(TestRunStarted.class, e -> VirtualEnv.setUp());
    }
  }
}
