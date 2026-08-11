package org.atsign.client.impl.cli.register;

import org.atsign.client.impl.exceptions.AtRegistrarException;

import java.util.Map;
import org.atsign.client.api.AtSign;


class RegisterAtsign extends RegisterApiTask<RegisterApiResult<Map<String, String>>> {

  @Override
  public RegisterApiResult<Map<String, String>> run() {
    System.out.println("Sending verification code to: " + params.get("email"));
    try {
      result.data.put("otpSent",
                      registerUtil.registerAtsign(params.get("email"), AtSign.of(params.get("atSign")),
                                                  params.get("registrarUrl"), params.get("apiKey"))
                          .toString());
      result.apiCallStatus = ApiCallStatus.success;
    } catch (Exception e) {
      result.atException = new AtRegistrarException(e.getMessage(), e.getCause());
      result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
    }
    return result;
  }
}
