package org.atsign.common;

/**
 * Stores processed data from Registrar API response
 * 
 * @data stores actual information extracted from API response
 * @apiCallStatus stores the present status of the API called
 * @atException stores the exception captured in case one occured
 */
public class RegisterApiResult<T> {

    public T data;

    public ApiCallStatus apiCallStatus;

    public AtException atException;
}
