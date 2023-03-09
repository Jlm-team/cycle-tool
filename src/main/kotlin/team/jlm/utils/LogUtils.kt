package team.jlm.utils

import com.intellij.openapi.diagnostic.Logger

fun Logger.debug(message: () -> Any?) {
    if (isDebugEnabled) {
        debug(message().toString())
    }
}