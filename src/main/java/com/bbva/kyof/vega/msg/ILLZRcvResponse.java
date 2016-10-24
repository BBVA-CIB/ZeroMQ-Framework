package com.bbva.kyof.vega.msg;

import java.util.UUID;

/**
 * Interface that represents received response.</br></br>
 *
 * The contents of the request comes in the form of a ByteBuffer, the internal buffer can be a direct or non direct buffer,
 * don't try to access the buffer internal array directly in any case!</br></br>
 *
 * The request ByteBuffer contents may be reused by the framework to avoid memory allocation, always promote the message
 * if the message contents are going to be accessed on separate user thread!
 */
public interface ILLZRcvResponse extends ILLZRcvMessage
{
    /** Get the unique request identifier of the original sent request */
    UUID getOriginalRequestId();
}
