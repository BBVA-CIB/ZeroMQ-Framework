package com.bbva.kyof.vega.msg;


import java.nio.ByteBuffer;

/**
 * Represent a received message.
 *
 * This class is not thread safe!
 */
public class LLZRcvMessage implements ILLZRcvMessage
{
    /** Header of the message with the message metadata information */
    protected LLZMsgHeader header;

    /** Contents of the message represented as a byte buffer */
    protected ByteBuffer content;

    /** Topic name the message belongs to, in internal framework messages it may not contain a proper topic string.
     *  Doesn't travel across network 
     */
    private final String topicName;
    

    /**
     * Create a new message given the trailer and the content of the message
     *  @param header the trailer with he message metadata information
     * @param content the binary contents of the message
     * @param topicName
     */
    public LLZRcvMessage(final LLZMsgHeader header, final ByteBuffer content, String topicName)
    {
        this.header = header;
        this.content = content;
        this.topicName = topicName;
    }
  
    /** @return the message internal type */
    public LLZMsgType getMessageType()
    {
        return this.header.getMsgType();
    }

    @Override
    public String getTopicName()
    {
        return this.topicName;
    }
    
    @Override
    public String getVersion()
    {
        return this.header.getVersion();
    }
    
    /** @return topic internal id (long) */
    public Long getTopicId() {
        return this.header.getTopicUniqueId();
    }

    @Override
    public long getInstanceId()
    {
        return this.header.getInstanceId();
    }

    @Override
    public ByteBuffer getMessageContent()
    {
        return this.content;
    }
    
    @Override
    public void promote()
    {
        final ByteBuffer resultBuffer = ByteBuffer.allocate(this.content.limit() - this.content.position());
        this.promote(resultBuffer);
    }

    @Override
    public void promote(final ByteBuffer newBuffer)
    {
        if (newBuffer.isDirect())
        {
            throw new IllegalArgumentException("Direct byte buffers are not supported");
        }

        // Store the current internal buffer status
        this.content.mark();

        // Copy the contents
        newBuffer.put(this.content);
        newBuffer.flip();

        // Restore current internal buffer status
        this.content.reset();

        // Change the internal buffer for the provided one
        this.content = newBuffer;
    }

    /**
     * Set the message contents byte buffer
     *
     * @param messageContents the new contents
     */
    public void setMessageContents(final ByteBuffer messageContents)
    {
        this.content = messageContents;
    }
}
