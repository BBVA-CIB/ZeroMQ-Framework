package com.bbva.kyof.vega.config;

/**
 * Constants for the configuration readers
 */
public final class LLZConfigReaderConstants
{
    /**Static literal to build the jaxb schema url*/
    public static final String HTTP = "http";
    
    /**Static literal to build the jaxb schema*/
    public static final String PREFIX_FILE = "://";
    
    /** Schema used to validate the xsd for the jaxb */
    public static final String W3_SCHEMA = HTTP + PREFIX_FILE + "www.w3.org/2001/XMLSchema";
    
    /** Private constructor to prevent instantiation of constants class */
    private LLZConfigReaderConstants()
    {
        // Nothing to do here
    }
}
