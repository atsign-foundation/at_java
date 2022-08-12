package org.atsign.common;

public class Result<T> {
    public T data;
    public ApiCallStatus apiCallStatus;
    public Exception exception;
}
