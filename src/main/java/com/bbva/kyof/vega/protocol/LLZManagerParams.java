package com.bbva.kyof.vega.protocol;


/**
 * Parameters for the ZeroMQ framework instance initialization
 */
public final class LLZManagerParams
{
    /** Name of the instance */
    private final String instanceName;
    
    /** path + name of the configuration xml file */
    private final String configurationFile;
    
    /** (Optional) path with the ZMQ native libraries, it can contain several directories separated by ':' or ';' */
    private final String zmqLibraryPath;
    
    /** (Optional) number of thread that the 0MQ library will use to perform all the I/O operations */
    private final int numberOfThreads;

    /**
     * Configuration builder, it follows the builder pattern to create unmodificable instances of the configuration
     */
    public static class Builder
    {
        /** Name of the application */
        private final String instanceName;

        /** Configuration file containing the ZeroMQ framework configuration */
        private final String configurationFile;

        /** (Optional) path with the ZMQ native libraries, , it can contain several directories separated by ':' or ';' */
        private String zmqLibraryPath = null;
        
        /** (Optional) number of thread that the 0MQ library will use to perfroms all the I/O operations */
        private int numberOfThreads = 1;

        /**
         * This function initializes the basic values which will be used by the framework to perform all the operations
         * 
         * @param instanceName        Identifier of the context.
         * @param configurationFile The xml configuration file.
         */
        public Builder(final String instanceName, final String configurationFile)
        {
            this.instanceName = instanceName;
            this.configurationFile  = configurationFile;
        }

        /**
         * This function initializes the basic values which will be used by the framework to perform all the operations
         * Uses empty configuration file and instanceID 0
         */
        public Builder()
        {
            this.configurationFile  = "xml/emptyConfiguration.xml";
            this.instanceName = "";
        }

        /**
         * Crates the LLZManagerParams instance.
         * 
         * @return An instance of {@link LLZManagerParams}
         */
        public LLZManagerParams build()
        {
            return new LLZManagerParams(this);
        }

        /**
         * Set the path programatically to the Native ZMQ libraries
         *
         * @param zmqLibraryPath the path to the libraries, it can contain several directories separated by ':' or ';'
         * @return the Builder object
         */
        public Builder zmqLibraryPath(final String zmqLibraryPath)
        {
            this.zmqLibraryPath = zmqLibraryPath;
            return this;
        }

        /**
         * This function sets the number of thread the user wants the native library (zeromq) to use in their operations
         * 
         * @param numberOfThreads Number of threads to set.
         * @return An instance of {@link LLZManagerParams.Builder}
         */
        public Builder numberOfThreads(final int numberOfThreads)
        {
            this.numberOfThreads = numberOfThreads;
            return this;
        }

    }    
   
    /**
     * Constructor of the LLZManagerParams class.
     * 
     * @param builder object
     */
    private LLZManagerParams(final Builder builder)
    {
        // Required parameters
        this.configurationFile  = builder.configurationFile;
        this.instanceName       = builder.instanceName;   
        this.zmqLibraryPath     = builder.zmqLibraryPath;
        this.numberOfThreads    = builder.numberOfThreads;
    }

    /**
     * Returns the number of threads that the Library will create to perform I/O operations
     *
     * @return manager Name value
     */
    public String getInstanceName()
    {
        return this.instanceName;
    }

    /**
     * Returns the configuration file passed.
     * 
     * @return The name of the configuration file.
     */
    public String getConfigurationFile()
    {
        return this.configurationFile;
    }

    /**
     * Return the library path containing the ZMQ native libraries if it has been settled programatically
     * 
     * @return the path to the native ZMQ libraries, it can contain several directories separated by ':' or ';'
     */
    public String getZmqLibraryPath()
    {
        return this.zmqLibraryPath;
    }

    /**
     * Returns the number of threads set.
     * 
     * @return Number of threads.
     */
    public int getNumberOfThreads()
    {
        return this.numberOfThreads;
    }

    
    @Override
    public String toString()
    {
        return "LLZManagerParams{" +
                "configurationFile='" + this.configurationFile + '\'' +
                ", instanceName=" + this.instanceName +
                ", zmqLibraryPath='" + this.zmqLibraryPath + '\'' +
                ", numberOfThreads=" + this.numberOfThreads +
                '}';
    }
}
