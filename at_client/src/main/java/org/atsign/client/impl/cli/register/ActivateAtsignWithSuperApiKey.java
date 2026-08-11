package org.atsign.client.impl.cli.register;

import org.atsign.client.impl.exceptions.AtRegistrarException;

import java.util.Map;
import org.atsign.client.api.AtSign;


class ActivateAtsignWithSuperApiKey extends RegisterApiTask<RegisterApiResult<Map<String, String>>> {
  @Override
  public RegisterApiResult<Map<String, String>> run() {
    try {
      result.data.put(
                      "cram", registerUtil
                          .activateAtsignWithSuperApiKey(params.get("registrarUrl"), params.get("apiKey"),
                                                         AtSign.of(params.get("atSign")),
                                                         params.get("ActivationKey"))
                          .split(":")[1]);
      result.apiCallStatus = ApiCallStatus.success;
      System.out.println("Your cram secret: " + result.data.get("cram"));
    } catch (AtRegistrarException e) {
      result.atException = e;
      result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
    } catch (Exception e) {
      result.atException = new AtRegistrarException("Failed while activating atSign", e);
      result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
    }
    return result;
  }
}
