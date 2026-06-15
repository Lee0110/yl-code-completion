package com.lyl.ylcodecompletion.llm;

public class LlmException extends RuntimeException {

    public enum Kind {
        AUTH,
        RATE_LIMITED,
        SERVER,
        BAD_REQUEST,
        TIMEOUT,
        NETWORK,
        UNKNOWN
    }

    private final Kind kind;

    public LlmException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public LlmException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
