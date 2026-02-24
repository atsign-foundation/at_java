package org.atsign.client.connection.protocol;

import static org.atsign.client.connection.protocol.Error.throwExceptionIfError;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.common.AtException;
import org.atsign.common.VerbBuilders;

/**
 * Atsign protocol utility code that relates querying the keys in an atserver.
 *
 */

public class Scan {

  public static List<String> scan(AtClientConnection connection, boolean showHidden, String regex) throws AtException {
    try {

      // send scan command
      String scanCommand = VerbBuilders.scanCommandBuilder()
          .showHidden(showHidden)
          .regex(regex)
          .build();
      String scanResponse = connection.sendSync(scanCommand);

      // return unmarshalled key names
      return Data.matchDataJsonListOfStrings(throwExceptionIfError(scanResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
