package org.schabi.newpipe.extractor.utils

/**
 * Interface to execute JavaScript code.
 *
 * Some clients, such as the WEB player, require to execute JavaScript code to allow playback.
 */
public fun interface JavaScriptInterpreter {

    /**
     * Runs the specified function in the code.
     *
     * @param code the code that contains the function that should be run
     * @param functionName the name of the function that should be run
     * @param parameters the parameters to run with the function
     * @return the result of the run function
     */
    public fun run(code: String, functionName: String, vararg parameters: String): String
}
