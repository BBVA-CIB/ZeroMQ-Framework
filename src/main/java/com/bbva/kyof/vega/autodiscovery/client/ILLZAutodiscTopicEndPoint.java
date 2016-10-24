package com.bbva.kyof.vega.autodiscovery.client;

/**
 * Interface that represent the auto-discovery information of a topic end-point
 */
public interface ILLZAutodiscTopicEndPoint
{
    /**
     * Get unique topic name
     *
     * @return topic name
     */
    String getTopicName();

    /**
     * Get unique socket id
     *
     * @return Unique socket id which represents a socket
     */
    Long getSocketId();

    /**
     * Get unique socket Topic id
     *
     * @return Unique socket topic id which represents a socket+topic pair
     */
    Long getTopicId();

    /**
     *  Get unique App Id
     * @return
     */
    Long getInstanceId();

    /**
     * Get publisher address
     *
     * @return publisher address
     */
    String getBindAddress();

    /**
     * Returns the end point type
     *
     * @return the type of the end-point
     */
    LLZAutodiscEndPointType getType();
}