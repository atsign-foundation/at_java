package org.atsign.client.impl.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.impl.exceptions.AtUnauthenticatedException;
import org.junit.jupiter.api.Test;

class ErrorResponsesTest {

  @Test
  public void testError() {
    assertThat(ErrorResponses.matchError("error:fail"), is("fail"));
    assertThat(ErrorResponses.matchError("error:some reason"), is("some reason"));
    assertThat(ErrorResponses.matchError("error:AT0401:an error message"), is("AT0401:an error message"));
  }

  @Test
  public void testThrowExceptionIfErrorDoesNothingIfNoError() throws AtException {
    ErrorResponses.throwExceptionIfError("data:success");
    ErrorResponses.throwExceptionIfError("");
  }

  @Test
  public void testThrowExceptionIfErrorThrowsTypedException() {
    Exception ex =
        assertThrows(Exception.class, () -> ErrorResponses.throwExceptionIfError("error:AT0401:an error message"));
    assertThat(ex, instanceOf(AtUnauthenticatedException.class));
    assertThat(ex.getMessage(), containsString("an error message"));
  }

  @Test
  public void testThrowExceptionIfErrorThrowsExceptionIfNull() {
    Exception ex = assertThrows(Exception.class, () -> ErrorResponses.throwExceptionIfError(null));
    assertThat(ex, instanceOf(IllegalArgumentException.class));
  }

}
