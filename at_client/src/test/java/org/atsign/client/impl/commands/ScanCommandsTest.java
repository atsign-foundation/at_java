package org.atsign.client.impl.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.impl.exceptions.AtServerRuntimeException;
import org.junit.jupiter.api.Test;

class ScanCommandsTest {

  @Test
  void testScanReturnsExpectedResults() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("scan:showHidden:true .*", "data:[]")
        .build();

    assertThat(ScanCommands.scan(executor, true, ".*"), equalTo(List.of()));

    executor = TestExecutorBuilder.builder()
        .stub("scan:showHidden:true .+", "data:[\"public:publickey@gary\",\"public:signing_publickey@gary\"]")
        .build();

    assertThat(ScanCommands.scan(executor, true, ".+"),
               equalTo(List.of("public:publickey@gary", "public:signing_publickey@gary")));

    executor = TestExecutorBuilder.builder()
        .stub("scan .*", "data:[\"public:publickey@gary\"]")
        .build();
    assertThat(ScanCommands.scan(executor, false, ".*"),
               equalTo(List.of("public:publickey@gary")));
  }

  @Test
  void testScanThrowsServerException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("scan:showHidden:true .*", "error:AT0001:deliberate")
        .build();
    assertThrows(AtServerRuntimeException.class, () -> ScanCommands.scan(executor, true, ".*"));
  }


  @Test
  void testScanThrowsExecutionException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubExecutionException("scan:showHidden:true .*")
        .build();
    assertThrows(RuntimeException.class, () -> ScanCommands.scan(executor, true, ".*"));
  }

}
