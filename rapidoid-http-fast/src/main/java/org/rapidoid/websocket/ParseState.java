package org.rapidoid.websocket;

/**
 * Created by randy on 7/31/16.
 */
public enum ParseState {
    START,
    FIN_READ,
    MASK_READ,
    LEN_READ,
    LEN_PARSE_STARTED,
    LEN_PARSE_FIN,
    MASK_READ_STARTED,
    MASK_READ_FIN,
    PAYLOAD_READ_STARTED,
    PAYLOAD_READ_FIN,
    COMPLETE
}
