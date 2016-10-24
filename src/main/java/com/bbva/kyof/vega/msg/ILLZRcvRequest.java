package com.bbva.kyof.vega.msg;

import com.bbva.kyof.vega.exception.LLZException;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Interface that represents received request.</br></br>
 *
 * The contents of the request comes in the form of a ByteBuffer, the internal buffer can be a direct or non direct buffer,
 * don't try to access the buffer internal array directly in any case!</br></br>
 *
 * The request ByteBuffer contents may be reused by the framework to avoid memory allocation, always promote the message
 * if the message contents are going to be accessed on separate user thread!
 */
public interface ILLZRcvRequest extends ILLZRcvMessage
{
    /** Get the unique request identifier of the received request */
    UUID getRequestId();

    /**
     * Send a new response. There is no limit of responses that can be sent for the same received request.
     *
     * @param responseContent the response contents to send, it will send from the given buffer position to the given buffer limit.
     *                        Support direct and non direct byte buffers.
     *
     * @throws LLZException an exception may occur if the publisher has already been closed when the method to send response is called
     */
    void sendResponse(final ByteBuffer responseContent) throws LLZException;
}
