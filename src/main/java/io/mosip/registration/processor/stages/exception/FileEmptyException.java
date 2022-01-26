package io.mosip.registration.processor.stages.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class FileEmptyException extends BaseCheckedException  {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public FileEmptyException() {
        super();
    }

    /**
     * Instantiates a new reg proc checked exception.
     *
     * @param errorCode    the error code
     * @param errorMessage the error message
     */
    public FileEmptyException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}
