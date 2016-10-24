package com.bbva.kyof.vega.unit.serialization;

import com.bbva.kyof.vega.serialization.UUIDSerializer;
import junit.framework.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Test the methods of the class UUIDSerializer
 */
public class UUIDSerializerTest
{
    @Test
    public void testConstructor() throws Exception
    {
        Constructor<?>[] cons = UUIDSerializer.class.getDeclaredConstructors();
        cons[0].setAccessible(true);
        cons[0].newInstance((Object[]) null);
    }

    @Test
    public void testUniqueIdFromBinary() throws Exception
    {
        final UUID testUUID = UUID.randomUUID();
        final ByteBuffer resultBuffer = ByteBuffer.allocate(128);

        UUIDSerializer.uniqueIdToBinary(testUUID, resultBuffer);

        resultBuffer.flip();

        final UUID readedUUID = UUIDSerializer.uniqueIdFromBinary(resultBuffer);

        Assert.assertEquals(testUUID, readedUUID);

        // Finally test the byte array method
        final byte[] serializedArray = UUIDSerializer.uniqueIdToByteArray(testUUID);

        UUIDSerializer.uniqueIdFromBinary(ByteBuffer.wrap(serializedArray));

        Assert.assertEquals(testUUID, readedUUID);

        // Finally check sizes
        Assert.assertTrue(serializedArray.length == UUIDSerializer.uniqueIdSerializedSize());
    }

    @Test
    public void testUniqueIdToBinary() throws Exception
    {

    }

    @Test
    public void testUniqueIdToByteArray() throws Exception
    {

    }

    @Test
    public void testUniqueIdSerializedSize() throws Exception
    {

    }
}