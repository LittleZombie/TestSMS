package com.test.sms

import androidx.annotation.StringDef

class SmsResult {

    companion object {
        const val SUCCESS = "success"
        const val FAIL = "fail"
        const val TIMEOUT = "timeout"
    }

    @StringDef(SUCCESS, FAIL, TIMEOUT)
    annotation class Type

}