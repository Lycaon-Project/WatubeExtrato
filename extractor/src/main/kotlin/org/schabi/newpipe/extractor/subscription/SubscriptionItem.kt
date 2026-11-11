package org.schabi.newpipe.extractor.subscription

import java.io.Serializable

/**
 * A subscription read from an export or a channel. Still a JVM record, so Java code keeps
 * calling `url()`, `name()` and `serviceId()`.
 */
@JvmRecord
public data class SubscriptionItem(
    val serviceId: Int,
    val url: String,
    val name: String,
) : Serializable {

    /** Without the channel URL and name: a subscription list is personal data. */
    override fun toString(): String = "SubscriptionItem[serviceId=$serviceId]"

    public companion object {
        private const val serialVersionUID: Long = 1L

        @JvmField
        public val INVALID: SubscriptionItem = SubscriptionItem(Int.MIN_VALUE, "Invalid", "Invalid")
    }
}
