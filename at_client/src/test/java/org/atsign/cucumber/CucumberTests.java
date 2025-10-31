package org.atsign.cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestRunStarted;
import org.atsign.virtualenv.VirtualEnv;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"org.atsign.cucumber.steps"},
    plugin = {"pretty", "org.atsign.cucumber.CucumberTests"}
)
public class CucumberTests implements EventListener {

  @Override
  public void setEventPublisher(EventPublisher publisher) {
    publisher.registerHandlerFor(TestRunStarted.class, e -> VirtualEnv.setUp());
  }
}
