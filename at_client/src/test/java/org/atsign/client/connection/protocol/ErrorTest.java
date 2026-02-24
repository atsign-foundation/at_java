package org.atsign.client.connection.protocol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.atsign.common.AtException;
import org.atsign.common.exceptions.AtUnauthenticatedException;
import org.junit.jupiter.api.Test;

class ErrorTest {

  @Test
  public void testError() {
    assertThat(Error.matchError("error:fail"), is("fail"));
    assertThat(Error.matchError("error:some reason"), is("some reason"));
    assertThat(Error.matchError("error:AT0401:an error message"), is("AT0401:an error message"));
  }

  @Test
  public void testThrowExceptionIfErrorDoesNothingIfNoError() throws AtException {
    Error.throwExceptionIfError("data:success");
    Error.throwExceptionIfError("");
  }

  @Test
  public void testThrowExceptionIfErrorThrowsTypedException() {
    Exception ex = assertThrows(Exception.class, () -> Error.throwExceptionIfError("error:AT0401:an error message"));
    assertThat(ex, instanceOf(AtUnauthenticatedException.class));
    assertThat(ex.getMessage(), containsString("an error message"));
  }

  @Test
  public void testThrowExceptionIfErrorThrowsExceptionIfNull() {
    Exception ex = assertThrows(Exception.class, () -> Error.throwExceptionIfError(null));
    assertThat(ex, instanceOf(IllegalArgumentException.class));
  }

}
