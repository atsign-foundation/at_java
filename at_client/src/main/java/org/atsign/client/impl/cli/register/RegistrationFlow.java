package org.atsign.client.impl.cli.register;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class RegistrationFlow {
  List<RegisterApiTask<RegisterApiResult<Map<String, String>>>> processFlow = new ArrayList<>();
  RegisterApiResult<Map<String, String>> result;
  Map<String, String> params;
  RegisterUtil registerUtil = new RegisterUtil();

  RegistrationFlow(Map<String, String> params) {
    this.params = params;
  }

  RegistrationFlow add(RegisterApiTask<RegisterApiResult<Map<String, String>>> task) {
    processFlow.add(task);
    return this;
  }

  void start() throws Exception {
    for (RegisterApiTask<RegisterApiResult<Map<String, String>>> task : processFlow) {
      // initialize each task by passing params to init()
      task.init(params, registerUtil);
      result = task.run();
      if (result.apiCallStatus.equals(ApiCallStatus.retry)) {
        while (task.shouldRetry()
            && result.apiCallStatus.equals(ApiCallStatus.retry)) {
          result = task.run();
          task.retryCount++;
        }
      }
      if (result.apiCallStatus.equals(ApiCallStatus.success)) {
        params.putAll(result.data);
      } else {
        throw result.atException;
      }
    }
  }
}
