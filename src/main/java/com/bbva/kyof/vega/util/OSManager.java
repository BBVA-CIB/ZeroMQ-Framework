package com.bbva.kyof.vega.util;

import java.util.Locale;

/**
 * This class helps to detect the current OS and have some helper methods to get the right constants depending on the OS
 */
public class OSManager
{
    /** Singletone instance of the manager */
    private static final OSManager INSTANCE = new OSManager();

    /** Windows separator for library path entries */
    public static final String WIN_PATH_SEP = ";";
    /** Linux / Mac / Solaris separator for library path entries */
    public static final String LINUX_PATH_SEP = ":";

    /** Cached result of OS detection */
    private final OSType detectedOS;

    /**
     * Create the Manager Instance and check about the OS type on creation
     */
    public OSManager()
    {
        final String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);

        if (osName.contains("mac"))
        {
            this.detectedOS = OSType.MACOS;
        }
        else if (osName.contains("darwin"))
        {
            this.detectedOS = OSType.MACOS;
        }
        else
        {
            if (osName.contains("win"))
            {
                this.detectedOS = OSType.WINDOWS;
            }
            else if (osName.contains("nux"))
            {
                this.detectedOS = OSType.LINUX;
            }
            else
            {
                this.detectedOS = OSType.OTHER;
            }
        }
    }

    /**
     * Return the singletone instance of the manager
     *
     * @return the manager instance
     */
    public static OSManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * Return the detected Operative System
     *
     * @return - the operating system detected
     */
     public OSType getOperatingSystemType()
    {
        return this.detectedOS;
    }

    /**
     * Returns the separator for multiple directories in the library path depending on the OS
     * @return the separator for multiple directories in the library path depending on the OS
     */
    public String getLibraryPathSeparator()
    {
        if (this.detectedOS == OSType.WINDOWS)
        {
            return  WIN_PATH_SEP;
        }
        else
        {
            return LINUX_PATH_SEP;
        }
    }
}
