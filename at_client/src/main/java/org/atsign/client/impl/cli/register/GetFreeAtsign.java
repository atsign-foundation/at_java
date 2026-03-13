package org.atsign.client.impl.cli.register;

import org.atsign.client.impl.exceptions.AtRegistrarException;

import java.util.Map;

class GetFreeAtsign extends RegisterApiTask<RegisterApiResult<Map<String, String>>> {

  @Override
  public RegisterApiResult<Map<String, String>> run() {
    System.out.println("Fetching free atsign ...");
    try {
      result.data.put("atSign",
                      registerUtil.getFreeAtsign(params.get("registrarUrl"), params.get("apiKey")));
      result.apiCallStatus = ApiCallStatus.success;
      System.out.println("\tFetched createAtSign: " + "@" + result.data.get("atSign"));
    } catch (AtRegistrarException e) {
      result.atException = e;
    } catch (Exception e) {
      result.atException = new AtRegistrarException("error while getting free atsign", e);
      result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
    }
    return result;
  }
}
