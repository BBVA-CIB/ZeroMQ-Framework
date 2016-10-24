package com.bbva.kyof.vega.msg;

import java.nio.ByteBuffer;

/**
 * Interface that represents received message.</br></br>
 *
 * The contents of the message comes in the form of a ByteBuffer, the internal buffer can be a direct or non direct buffer,
 * don't try to access the buffer internal array directly in any case!</br></br>
 *
 * The message ByteBuffer contents may be reused by the framework to avoid memory allocation, always promote the message
 * if the message contents are going to be accessed on separate user thread!
 */
public interface ILLZRcvMessage
{
    /** Get the topic name of the received message */
    String getTopicName();

    /** Get the unique instance ID of the application that sent the message */
    long getInstanceId();
    
    /** Get the Framework version of the application that send the received message */
    String getVersion();
    
    /** Retrieves buffer with the contents of the message. </br></br>
     * The contents may contain the internal framework header in some cases. The position of the buffer will always be
     * just after the framework header*/
    ByteBuffer getMessageContent();

    /**
     * Promote the message by cloning the message contents into a new internal ByteBuffer.
     *
     * Since the framework will try to reuse internal buffers you have to promote the message if it's contents are going
     * to be accessed from a separate thread.
     */
    void promote();

    /**
     * Promote the message by copying the message contents into the given byte buffer. The given byte buffer will be used
     * as the new message contents.
     *
     * This method is available to give the user the chance to reuse buffers even if the message has to be cloned to be
     * used in a separate thread.
     *
     * Since the framework will try to reuse internal buffers you have to promote the message if it's contents are going
     * to be accessed from a separate thread.
     *
     * @param newBuffer buffer that will be used to clone the message contents. Direct buffers are not supported!
     */
    void promote(final ByteBuffer newBuffer);
}
