package org.atsign.common;

import java.util.HashMap;
import java.util.Map;

import org.atsign.client.util.RegisterUtil;

/**
 * Represents a task in an AtSign registration cycle
 */
public abstract class RegisterApiTask<V> {

    public static final int maxRetries = 3;

    public int retryCount = 0;

    public Map<String, String> params;

    public RegisterApiResult<Map<String, String>> result = new RegisterApiResult<Map<String, String>>();

    protected RegisterUtil registerUtil;

    /**
     * Initializes the Task object with necessary parameters
     * 
     * @param params       map that contains necessary data to complete atsign
     *                     registration process
     * @param registerUtil shared utility class object containing necessary methods
     *                     to complete registration
     */
    public void init(Map<String, String> params, RegisterUtil registerUtil) {
        this.params = params;
        this.registerUtil = registerUtil;
        this.result.data = new HashMap<String, String>();
    }

    /**
     * Child classes need to implement required logic in this method to complete
     * their sub-process in the AtSign registration process
     * 
     * @return {@link #result} object with necessary information collected in the
     *         corresponding API call
     */
    abstract public V run();

    /**
     * In case the task has returend an {@link #ApiCallStatus} of retry, this method
     * is
     * called. Re-runs the task.
     * 
     * @return {@link #result} object with necessary information collected in the
     *         corresponding retry of the API call
     */
    public boolean shouldRetry() {
        return retryCount < maxRetries;
    }

}
