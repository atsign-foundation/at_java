package org.atsign.common;

import java.util.Map;
import org.atsign.client.util.RegisterUtil;

public abstract class Task<V> {
    public static final int maxRetries = 3;

    public int retryCount = 0;

    protected Map<String, String> params;

    protected RegisterUtil registerUtil;

    protected Result<Map<String, String>> result = new Result<Map<String, String>>();

    public void init(Map<String, String> params, RegisterUtil registerUtil){
        this.params = params;
        this.registerUtil = registerUtil;
    }

    abstract public V run();

    public V retry() {
        retryCount++;
        return run();
    }

}
