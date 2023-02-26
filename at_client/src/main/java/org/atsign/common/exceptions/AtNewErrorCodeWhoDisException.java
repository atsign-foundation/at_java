package org.atsign.common.exceptions;

import org.atsign.common.AtException;

public class AtNewErrorCodeWhoDisException extends AtException {
            public AtNewErrorCodeWhoDisException(String errorCode, String message) {
                super(errorCode + ":" + message);
            }
        }

