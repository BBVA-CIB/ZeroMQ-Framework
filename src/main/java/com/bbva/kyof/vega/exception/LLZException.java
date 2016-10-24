package com.bbva.kyof.vega.exception;


/**
 * ZMQ Framework library exception type
 */
public class LLZException extends java.lang.Exception
{
    /** Exception code */
    private final LLZExceptionCode exceptionCode;

    /**
     * Constructor with an exception code and a message
     *
     * @param customMsg the message for the exception
     */
    public LLZException(final String customMsg)
    {
        this(customMsg, LLZExceptionCode.UNDEFINED);
    }

    /**
     * Constructor with an exception code, and cause of the exception
     *
     * @param cause the cause of the exception
     */
    public LLZException(final Throwable cause)
    {
        this(cause, LLZExceptionCode.UNDEFINED);
    }

    /**
     * Constructor with an exception code, message and cause of the exception
     *
     * @param cause the cause of the exception
     * @param customMessage the message for the exception
     */
    public LLZException(final String customMessage, final Throwable cause)
    {
        this(customMessage, cause, LLZExceptionCode.UNDEFINED);
    }

    /**
     * Constructor with an exception code and a message
     *
     * @param customMsg the message for the exception
     */
    public LLZException(final String customMsg, final LLZExceptionCode exceptionCode)
    {
        super(customMsg);
        this.exceptionCode = exceptionCode;
    }

    /**
     * Constructor with an exception code, and cause of the exception
     *
     * @param cause the cause of the exception
     */
    public LLZException(final Throwable cause, final LLZExceptionCode exceptionCode)
    {
        super(cause);
        this.exceptionCode = exceptionCode;
    }

    /**
     * Constructor with an exception code, message and cause of the exception
     *
     * @param cause the cause of the exception
     * @param customMessage the message for the exception
     */
    public LLZException(final String customMessage, final Throwable cause, final LLZExceptionCode exceptionCode)
    {
        super(customMessage, cause);
        this.exceptionCode = exceptionCode;
    }

    /**
     * @return the exception code
     */
    public LLZExceptionCode getExceptionCode()
    {
        return this.exceptionCode;
    }
}
