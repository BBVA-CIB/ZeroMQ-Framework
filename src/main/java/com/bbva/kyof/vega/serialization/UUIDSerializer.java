package com.bbva.kyof.vega.serialization;

import com.bbva.kyof.utils.serialization.bytebuffer.LLUSerializerLong;
import com.bbva.kyof.utils.serialization.bytebuffer.LLUSerializerUtils;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Utility class to serialize UUIDs
 */
public final class UUIDSerializer
{
    /** Private constructor to avoid instantiation of utility class */
    private UUIDSerializer()
    {
        // Nothing to do here
    }

    /**
     * Read the UUID from the binary contents stored in the given byte buffer
     *
     * @param buffer the buffer containing the serialized UUID
     * @return the result UUID
     */
    public static UUID uniqueIdFromBinary(final ByteBuffer buffer)
    {
        final long mostSignificantBits = LLUSerializerUtils.LONG.readFix(buffer);
        final long lessSignificantBits = LLUSerializerUtils.LONG.readFix(buffer);

        return new UUID(mostSignificantBits, lessSignificantBits);
    }

    /**
     * Serialize the given UUID into binary and store the result in the given ByteBuffer
     *
     * @param uniqueId the unique ID to serialize
     * @param buffer the buffer to write the result into
     */
    public static void uniqueIdToBinary(final UUID uniqueId, final ByteBuffer buffer)
    {
        LLUSerializerUtils.LONG.writeFix(uniqueId.getMostSignificantBits(), buffer);
        LLUSerializerUtils.LONG.writeFix(uniqueId.getLeastSignificantBits(), buffer);
    }

    /**
     * Convert serialize the given UUID into a byte array
     *
     * @param uniqueId the ID to serialize
     * @return a byte array with the serialized ID
     */
    public static byte[] uniqueIdToByteArray(final UUID uniqueId)
    {
        final ByteBuffer result = ByteBuffer.allocate(UUIDSerializer.uniqueIdSerializedSize());
        LLUSerializerUtils.LONG.writeFix(uniqueId.getMostSignificantBits(), result);
        LLUSerializerUtils.LONG.writeFix(uniqueId.getLeastSignificantBits(), result);
        return result.array();
    }

    /** @return the expected serialized UUID size  */
    public static int uniqueIdSerializedSize()
    {
        return LLUSerializerLong.FIX_SIZE * 2;
    }
}
