package org.atsign.client.impl.commands;

import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.impl.exceptions.AtException;

/**
 * Atsign protocol utility code that relates querying the keys in an atserver.
 *
 */

public class ScanCommands {

  public static List<String> scan(AtCommandExecutor executor, boolean showHidden, String regex) throws AtException {
    try {

      // send scan command
      String scanCommand = CommandBuilders.scanCommandBuilder()
          .showHidden(showHidden)
          .regex(regex)
          .build();
      String scanResponse = executor.sendSync(scanCommand);

      // return unmarshalled key names
      return DataResponses.matchDataJsonListOfStrings(throwExceptionIfError(scanResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
