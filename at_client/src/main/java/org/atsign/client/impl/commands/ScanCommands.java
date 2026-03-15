package org.atsign.client.impl.commands;

import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.impl.exceptions.AtException;

/**
 * At Protocol utility code that relates querying the keys in an atserver.
 *
 */

public class ScanCommands {

  /**
   * Sends the scan command and decodes the JSON response.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param showHidden scan command argument which controls whether hidden keys are returned.
   * @param regex scan command argument which will filter the keys that are returned.
   * @throws AtException If any of the commands fail or the key does not exist.
   */
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
