package com.gj.animalauto

import timber.log.Timber

/**
 * Created by dbro on 7/28/17.
 */
class TestTimberTree(val startTime: Long) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
        println("${System.currentTimeMillis() - startTime} : $message")
    }

    // Throwable stack printing has dependencies which are not Mocked
    // Use a simplified representation instead
    override fun e(t: Throwable?, message: String?, vararg args: Any) {
        var message = message
        if (t != null) {
            var errorMsg = t.javaClass.simpleName
            if (t.message != null) errorMsg += " : " + t.message

            if (message == null) {
                message = errorMsg
            } else {
                message += errorMsg
            }
        }
        super.e(message, *args)
    }

}