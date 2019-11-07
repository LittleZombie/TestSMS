package com.test.sms

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*


// https://dzone.com/articles/android-one-tap-sms-verification-with-the-sms-user
// https://developers.google.com/identity/sms-retriever/request
class MainActivity : AppCompatActivity(), SMSBroadcastReceiver.OnSMSListener {

    private val TAG = "test_sms"
    private var appSignatures = mutableListOf<String>()
    private var phoneNumber = "0975896366"
    private var receiver : SMSBroadcastReceiver? = null
    private var countTime = totalTime

    private val countDownTimer = object : CountDownTimer(totalTime, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            Log.i(TAG, "onTick millisUntilFinished = $millisUntilFinished")
            countTime -= 1000
            val second = countTime / 1000
            text_result.text = "剩下 $second 秒"
        }

        override fun onFinish() {
            Log.i(TAG, "onFinish")
        }
    }

    companion object {
        const val REQUEST_CREDENTIAL_PICKER = 321  // Set to an unused request code
        const val totalTime: Long = 5 * 60 * 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestHint()
        initReceiver()
        appSignatures = AppSignatureHelper(this).appSignatures


        text_signatures.text = "${appSignatures[0]}"

        button_send.setOnClickListener {
            startSMSListener()
        }

        text_sms_content.postDelayed({
            text_sms_content.text = generateSMSContent()
        }, 1500)

    }

    private fun startCountDownTimer() {
        countTime = totalTime
        countDownTimer.start()
    }

    private fun generateSMSContent(): String {
        return "<#> Dear User, Your h2 verification code is 102938. Thank you.\n ${appSignatures[0]}"
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
        receiver?.run {
            LocalBroadcastManager.getInstance(baseContext).unregisterReceiver(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CREDENTIAL_PICKER -> {
                if (Activity.RESULT_OK == resultCode && data != null) {
                    val credential = data.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
                    Log.i(TAG, "   credential -> ${Gson().toJson(credential)}")
                    credential?.id?.run {
                        text_phone.text = this
                        phoneNumber = this
                    }
                }
            }
        }
    }

    override fun onResult(@SmsResult.Type smsResult: String, code: String?) {
        receiver?.run {
            LocalBroadcastManager.getInstance(baseContext).unregisterReceiver(this)
        }

        when(smsResult) {
            SmsResult.SUCCESS -> {
                text_result.text = "Success : $code"
            }
            SmsResult.FAIL -> {
                text_result.text = "Fail"
            }
            SmsResult.TIMEOUT -> {
                Log.i(TAG, "onResult TIMEOUT")
                text_result.text = "Timeout"
            }
        }
    }

    private fun initReceiver() {
        receiver = SMSBroadcastReceiver(this@MainActivity)
    }

    // Construct a request for phone numbers and show the picker
    private fun requestHint() {
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .build()
        val credentialsClient = Credentials.getClient(this)
        val intent = credentialsClient.getHintPickerIntent(hintRequest)
        startIntentSenderForResult(
            intent.intentSender, REQUEST_CREDENTIAL_PICKER, null, 0, 0, 0
        )
    }

    private fun startSMSListener() {
        SmsRetriever.getClient(this)
            .startSmsRetriever()
            .addOnSuccessListener {
                // Successfully started retriever, expect broadcast intent
                Log.i(TAG, "startSMSListener OnSuccess")

                receiver?.run {
                    LocalBroadcastManager.getInstance(baseContext)
                        .registerReceiver(this, IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION))
                }
                startCountDownTimer()

            }.addOnFailureListener {
                // Failed to start retriever, inspect Exception for more details
                Log.i(TAG, "startSMSListener OnFailure : ${it.message}")
            }
    }

}
