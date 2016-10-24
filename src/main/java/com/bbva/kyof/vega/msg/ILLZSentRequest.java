package com.bbva.kyof.vega.msg;

import java.util.UUID;

/**
 * Interface that represent a request that has been sent.</br></br>
 *
 * The request will be automatically closed when it expires however is a good practice to perform a manual close if no
 * more responses are expected.</br></br>
 *
 * If the request was sent with timeout 0 it will never expire, on this case it will only be closed if manually closed.</br></br>
 *
 * No more responses for the request will be processed once the request has been closed.
 */
public interface ILLZSentRequest
{
    /** Unique ID of the sent request */
    UUID getRequestId();

    /** Returns true if the request has already expired */
    boolean hasExpired();

    /** Close the request, freeing the memory and preventing new responses from being processed */
    void closeRequest();

    /** True if the sent request has been already closed either by the user or due to expiration */
    boolean isClosed();

    /** Return the number of responses received for this request */
    int getNumberOfResponses();
}
