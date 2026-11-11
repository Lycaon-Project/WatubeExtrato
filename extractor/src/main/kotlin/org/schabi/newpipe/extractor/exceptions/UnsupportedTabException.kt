package org.schabi.newpipe.extractor.exceptions

/** Thrown for a channel tab a service does not know; tab names are fixed by the services. */
public class UnsupportedTabException(unsupportedTab: String?) :
    UnsupportedOperationException("Unsupported tab $unsupportedTab")
