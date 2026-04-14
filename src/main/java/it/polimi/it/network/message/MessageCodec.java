package it.polimi.it.network.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

/**
 * Serialises and deserialises {@link Message} objects to/from JSON strings.
 *
 * Each message is a single compact JSON line (no embedded newlines), making
 * it safe to use with BufferedReader.readLine() over a socket stream.
 *
 * Thread-safe: ObjectMapper is stateless after configuration.
 */
public final class MessageCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private MessageCodec() {}

    /**
     * Serialises a message to a compact JSON string.
     *
     * @param message the message to encode
     * @return single-line JSON string
     * @throws IOException if serialisation fails
     */
    public static String encode(Message message) throws IOException {
        return MAPPER.writeValueAsString(message);
    }

    /**
     * Deserialises a JSON string to a Message.
     *
     * @param json the JSON string to decode
     * @return the decoded message
     * @throws IOException if the JSON is malformed or the type is unknown
     */
    public static Message decode(String json) throws IOException {
        return MAPPER.readValue(json, Message.class);
    }
}
