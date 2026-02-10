package org.atsign.common;

/**
 * Registrar API response record
 *
 * @param <T> data type for the response record
 */
public class RegisterApiResult<T> {

  public T data;

  public ApiCallStatus apiCallStatus;

  public AtException atException;
}
