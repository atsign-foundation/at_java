package org.atsign.client.impl.cli.register;

import org.atsign.client.impl.exceptions.AtRegistrarException;

import java.util.Map;

class GetFreeAtsignWithSuperApiKey extends RegisterApiTask<RegisterApiResult<Map<String, String>>> {
  @Override
  public RegisterApiResult<Map<String, String>> run() {
    System.out.println("Getting atSign ...");
    try {
      result.data.putAll(registerUtil.getAtsignWithSuperApiKey(params.get("registrarUrl"), params.get("apiKey")));
      System.out.println("Got atsign: " + result.data.get("atSign"));
      result.apiCallStatus = ApiCallStatus.success;
    } catch (AtRegistrarException e) {
      result.atException = e;
      result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
    } catch (Exception e) {
      result.atException = new AtRegistrarException("Failed while getting atSign", e);
      result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
    }
    return result;
  }
}
