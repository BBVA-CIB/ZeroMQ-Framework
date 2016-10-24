package com.bbva.kyof.vega.autodiscovery.client;

/**
 * Created by cnebrera on 15/04/16.
 */
public enum LLZAutodiscEndPointType
{
    PUBLISHER(1),
    RESPONDER(2),
    UNKNOWN(256);

    private final int intValue;

    LLZAutodiscEndPointType(final int intValue)
    {
        this.intValue = intValue;
    }

    /** @return the value of the auto discovery endPoint type  */
    public int getIntValue()
    {
        return this.intValue;
    }

    
    /**
     * Gets an auto discovery endPoint type from a given value
     * 
     * @param value of the auto discovery endPoint type
     * @return The type related to 
     */
    public static LLZAutodiscEndPointType fromIntValue(final int value)
    {
        if (value == 1)
        {
            return PUBLISHER;
        }
        else if (value == 2)
        {
            return RESPONDER;
        }
        else
        {
            return UNKNOWN;
        }
    }
}
