package org.atsign.client.connection.protocol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.common.exceptions.AtExceptions.AtServerRuntimeException;
import org.junit.jupiter.api.Test;

class ScanTest {

  @Test
  void testScanReturnsExpectedResults() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("scan:showHidden:true .*", "data:[]")
        .build();

    assertThat(Scan.scan(connection, true, ".*"), equalTo(List.of()));

    connection = TestConnectionBuilder.builder()
        .stub("scan:showHidden:true .+", "data:[\"public:publickey@gary\",\"public:signing_publickey@gary\"]")
        .build();

    assertThat(Scan.scan(connection, true, ".+"),
               equalTo(List.of("public:publickey@gary", "public:signing_publickey@gary")));

    connection = TestConnectionBuilder.builder()
        .stub("scan .*", "data:[\"public:publickey@gary\"]")
        .build();
    assertThat(Scan.scan(connection, false, ".*"),
               equalTo(List.of("public:publickey@gary")));
  }

  @Test
  void testScanThrowsServerException() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("scan:showHidden:true .*", "error:AT0001:deliberate")
        .build();
    assertThrows(AtServerRuntimeException.class, () -> Scan.scan(connection, true, ".*"));
  }


  @Test
  void testScanThrowsExecutionException() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stubExecutionException("scan:showHidden:true .*")
        .build();
    assertThrows(RuntimeException.class, () -> Scan.scan(connection, true, ".*"));
  }

}
