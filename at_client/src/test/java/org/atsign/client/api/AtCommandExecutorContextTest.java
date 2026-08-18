package org.atsign.client.api;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AtCommandExecutorContextTest {

  @Test
  void testConfigIsAnEmptyMapWhenNotSupplied() {
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), null);

    assertThat(context.getConfig(), anEmptyMap());
  }

  @Test
  void testConfigIsAnEmptyMapWhenNull() {
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), null);

    assertThat(context.getConfig(), anEmptyMap());
  }

  @Test
  void testConfigCannotBeModified() {
    Map<String, Object> config = new HashMap<>();
    config.put("clientId", "abc");
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), null, config);

    assertThrows(UnsupportedOperationException.class, () -> context.getConfig().put("clientId", "hijacked"));
  }

  @Test
  void testConfigIsCopiedSoLaterCallerChangesDoNotLeakIn() {
    Map<String, Object> config = new HashMap<>();
    config.put("clientId", "abc");
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), null, config);

    config.put("clientId", "changed");
    config.put("added", "later");

    // what the connection sends is fixed at construction
    assertThat(context.getConfig(), hasEntry("clientId", (Object) "abc"));
    assertThat(context.getConfig(), not(hasEntry("added", (Object) "later")));
  }

  @Test
  void testAnAtSignIsRequired() {
    // a context is a connection's identity, and every connection issues from:@atSign
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> new AtCommandExecutorContext(null, null));

    assertThat(ex.getMessage(), equalTo("atSign not set"));
  }

  @Test
  void testAContextHoldsAChallengeAtMostOnce() {
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), null);

    context.setChallenge("challenge");

    assertThat(context.consumeChallenge(), equalTo("challenge"));
    assertThat(context.consumeChallenge(), nullValue());
  }
}
