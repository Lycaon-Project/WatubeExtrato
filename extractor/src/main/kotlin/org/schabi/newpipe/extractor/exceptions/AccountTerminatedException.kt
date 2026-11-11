package org.schabi.newpipe.extractor.exceptions

public open class AccountTerminatedException : ContentNotAvailableException {

    /**
     * The reason for the violation. There should also be more info in the exception's message.
     */
    public val reason: Reason

    public constructor(message: String?) : super(message) {
        reason = Reason.UNKNOWN
    }

    public constructor(message: String?, reason: Reason) : super(message) {
        this.reason = reason
    }

    public constructor(message: String?, cause: Throwable?) : super(message, cause) {
        reason = Reason.UNKNOWN
    }

    public enum class Reason {
        UNKNOWN,
        VIOLATION,
    }
}
