package org.atsign.cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestRunStarted;
import org.atsign.cucumber.helpers.Helpers;
import org.atsign.virtualenv.VirtualEnv;
import org.junit.runner.RunWith;

import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"org.atsign.cucumber.steps"},
    plugin = {"pretty", "org.atsign.cucumber.CucumberTests"})
public class CucumberTests implements EventListener {

  @Override
  public void setEventPublisher(EventPublisher publisher) {
    if (!Helpers.isHostPortReachable("vip.ve.atsign.zone:64", SECONDS.toMillis(2))) {
      publisher.registerHandlerFor(TestRunStarted.class, e -> VirtualEnv.setUp());
    }
  }
}
