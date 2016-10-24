package com.bbva.kyof.vega.serialization;

import java.nio.ByteBuffer;

import com.bbva.kyof.utils.serialization.bytebuffer.LLUSerializerLong;
import com.bbva.kyof.utils.serialization.bytebuffer.LLUSerializerUtils;
import com.bbva.kyof.utils.serialization.model.LLUSerializationException;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.msg.LLZMsgHeader;
import com.bbva.kyof.vega.msg.LLZMsgType;
import com.bbva.kyof.vega.util.ThreadLocalBuffer;

/**
 * Helper class to serialize and deserialize the message header from and to binary.
 */
public final class LLZMsgHeaderSerializer
{
    /** Internal thread local buffer to ensure buffer reuse */
    private static final ThreadLocalBuffer THREAD_LOCAL_BUFFER = new ThreadLocalBuffer();

    /** Private constructor to avoid instantiation of utility class */
    private LLZMsgHeaderSerializer()
    {
        // Nothing to do here
    }

    /**
     * Serialize header and the given message into a ThreadLocal buffer.
     *
     * The thread local buffer will grow automatically if there is not enough space.
     *
     * @param header the header of the message to be added to the contents
     * @param msg the contents of the message
     *
     * @return a thread local buffer containing the header plus contents, already in position 0
     */
    public static ByteBuffer serializeHeaderAndMsgIntoReusableBuffer(final LLZMsgHeader header, final ByteBuffer msg) throws LLZException
    {
        // Mark the user msg to ensure positions are keep after serialization
        msg.mark();

        // Calculate required size
        final int headerSize = calculateHeaderSerializedSize(header);
        final int userMsgSize = msg.limit() - msg.position();

        // Get a big enough thread local buffer
        final ByteBuffer reusableBuffer = THREAD_LOCAL_BUFFER.getBuffer(headerSize + userMsgSize);

        // Serialize the header
        serializeHeader(header, reusableBuffer);

        // Now copy the message
        reusableBuffer.put(msg);

        // Restore user buffer status
        msg.reset();

        // Prepare the result buffer
        reusableBuffer.flip();

        return reusableBuffer;
    }

    /**
     * Serialize the header into the given buffer. If the buffer is not big enough it will launch a BufferOverflowException
     *
     * @param header the header to serialize
     * @param targetBuffer the buffer the header has to be added to
     * @throws LLZException exception thrown if there is an internal serialization problem
     */
    public static void serializeHeader(final LLZMsgHeader header, final ByteBuffer targetBuffer) throws LLZException
    {
        // Serialize the header fields
        try
        {
            LLZMsgHeaderSerializer.serializeHeaderFields(header, targetBuffer);
        }
        catch (final LLUSerializationException e)
        {
            throw new LLZException("Internal serialization error", e);
        }
    }

    /**
     * Creates a new byte array with the header serialized.
     *
     * @param header the header to serialize
     * @return the byte array containing the header
     * @throws LLZException if there is a problem during the serialization
     */
    public static byte[] serializeHeader(final LLZMsgHeader header) throws LLZException
    {
        // Serialize the header fields
        try
        {
            final ByteBuffer result = ByteBuffer.allocate(calculateHeaderSerializedSize(header));
            LLZMsgHeaderSerializer.serializeHeaderFields(header, result);
            return result.array();
        }
        catch (final LLUSerializationException e)
        {
            throw new LLZException("Internal serialization exception", e);
        }
    }

    /**
     * Serialize the internal header fields into the given buffer
     *
     * @param header the header which fields should be serialized
     * @param target the target buffer to serialize the fields into
     * @throws LLUSerializationException exception thrown when there is a problem with the serializers
     */
    private static void serializeHeaderFields(final LLZMsgHeader header, final ByteBuffer target) throws LLUSerializationException
    {
        // Add all the header fields into the buffer
        target.put(header.getMsgType().getByteValue()); // msgType
        LLUSerializerUtils.LONG.writeFix(header.getTopicUniqueId(), target); // topicUniqueId
        LLUSerializerUtils.LONG.writeFix(header.getInstanceId(), target); // instanceId
        LLUSerializerUtils.STRING.write(header.getVersion(), target); // Framework version
        
        // Serialize the optional request id
        if (header.getRequestId() != null)
        {
            // Add the boolean that indicates if there is a request ID field
            LLUSerializerUtils.BOOL.write(true, target);
            UUIDSerializer.uniqueIdToBinary(header.getRequestId(), target);
        }
        else
        {
            LLUSerializerUtils.BOOL.write(false, target);
        }
    }

    /**
     * Calculates the required size to serialize the header
     *
     * @param header the header to calculate the size for
     * @return the serialize size required if the header is serialized
     */
    public static int calculateHeaderSerializedSize(final LLZMsgHeader header)
    {
        int result = 1 + // Msg type
                     LLUSerializerLong.FIX_SIZE + // topicUniqueId
                     LLUSerializerLong.FIX_SIZE + // instanceId
                     LLUSerializerUtils.STRING.serializedSize(header.getVersion()) +  // Framework version
                     1; // Boolean so see if there is request id

        // Get the request ID size if settled
        if (header.getRequestId() != null)
        {
            result += UUIDSerializer.uniqueIdSerializedSize();
        }

        return result;
    }

    /**
     * Deserialize the header contained in the given buffer
     *
     * It will leave the position of the buffer just after the header
     *
     * @param buffer the buffer containing the message and the header
     * @return the deserialized header
     *
     * @throws LLZException exception thrown if there is a problem
     * @throws LLUSerializationException 
     */
    public static LLZMsgHeader deserializeHeader(final ByteBuffer buffer) throws LLZException, LLUSerializationException
    {
        final LLZMsgHeader result = new LLZMsgHeader();

        result.setMsgType(LLZMsgType.fromByte(buffer.get()));
        result.setTopicUniqueId(LLUSerializerUtils.LONG.readFix(buffer));
        result.setInstanceId(LLUSerializerUtils.LONG.readFix(buffer));
        result.setVersion(LLUSerializerUtils.STRING.read(buffer));

        final boolean hasRequestId = LLUSerializerUtils.BOOL.read(buffer);

        // Serialize the optional request id
        if (hasRequestId)
        {
            // Add the boolean that indicates if there is a request ID field
            result.setRequestId(UUIDSerializer.uniqueIdFromBinary(buffer));
        }
        else
        {
            result.setRequestId(null);
        }

        return result;
    }
}
