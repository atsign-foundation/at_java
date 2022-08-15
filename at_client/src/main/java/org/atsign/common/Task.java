package org.atsign.common;

import java.util.HashMap;
import java.util.Map;
import org.atsign.client.util.RegisterUtil;

public abstract class Task<V> {

    public static final int maxRetries = 3;

    public int retryCount = 0;

    public Map<String, String> params;

    public RegisterUtil registerUtil;

    public Result<Map<String, String>> result = new Result<Map<String, String>>();

    public void init(Map<String, String> params, RegisterUtil registerUtil){
        this.params = params;
        this.registerUtil = registerUtil;
        this.result.data = new HashMap<String, String>();
    }

    abstract public V run();

    public V retry() {
        retryCount++;
        return run();
    }

}
