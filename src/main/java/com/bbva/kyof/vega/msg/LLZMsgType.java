package com.bbva.kyof.vega.msg;

/** Describes all the available framework internal message types */
public enum LLZMsgType
{
    /** User message */
    DATA((byte)0),
    
    /** User request  message */
    DATA_REQ((byte)1),
    
    /** User response message */
    DATA_RESP((byte)2),
    
    /** Unknown message type */
    UNKNOWN((byte)127);

    /** Byte value that stores the type of message */
    private byte byteValue;

    
    /**
     * Create a new message type given the byte value
     * 
     * @param byteValue message type value in byte format
     */
    LLZMsgType(final byte byteValue)
    {
        this.byteValue = byteValue;
    }
   
    /**
     * Get the value in byte of the message type
     * 
     * @return value in byte of the message type
     */
    public byte getByteValue()
    {
        return this.byteValue;
    }
    
    /**
     * Creates a message type given a bytes value
     * 
     * @param byteValue value in byte of the message type
     * @return message type from the bytes value
     */
    public static LLZMsgType fromByte(final byte byteValue)
    {
        LLZMsgType result;

        switch (byteValue)
        {
            case 0:
                result = DATA;
                break;
            case 1:
                result = DATA_REQ;
                break;
            case 2:
                result = DATA_RESP;
                break;           
            default:
                result = UNKNOWN;
                break;
        }

        return result;
    }
}
