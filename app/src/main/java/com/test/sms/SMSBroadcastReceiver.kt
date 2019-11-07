package com.test.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import java.util.regex.Pattern

class SMSBroadcastReceiver(private var listener: OnSMSListener? = null) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            val bundle = intent.extras
            if (bundle != null) {
                val status = bundle.get(SmsRetriever.EXTRA_STATUS) as Status

                when (status.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        Log.d("test_sms", "SMSBroadcastReceiver SUCCESS")
                        val message = bundle.get(SmsRetriever.EXTRA_SMS_MESSAGE) as String
                        generateCode(message).run {
                            if (this?.isNotEmpty() == true) {
                                listener?.onResult(SmsResult.SUCCESS, this)
                            } else {
                                listener?.onResult(SmsResult.FAIL)
                            }
                        }
                    }
                    CommonStatusCodes.TIMEOUT -> {
                        Log.d("test_sms", "SMSBroadcastReceiver TIMEOUT")
                        listener?.onResult(SmsResult.TIMEOUT)
                    }
                }
            }
        }
    }

    private fun generateCode(message: String): String? {
        val pattern = Pattern.compile("\\d{6}")
        val matcher = pattern.matcher(message)
        return matcher.group(0).orEmpty()
    }

    interface OnSMSListener {
        fun onResult(@SmsResult.Type smsResult: String, code: String? = null)
    }

}