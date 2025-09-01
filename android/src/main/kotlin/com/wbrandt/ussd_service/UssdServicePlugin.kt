package com.wbrandt.ussd_service  // <- tu paquete

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class UssdServicePlugin : FlutterPlugin, MethodChannel.MethodCallHandler {

    private lateinit var channel: MethodChannel
    private lateinit var appContext: android.content.Context

    // ❌ onAttachedToEngine(FlutterEngineBinding)
    // ✅ onAttachedToEngine(FlutterPlugin.FlutterPluginBinding)
    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "com.wbrandt/ussd_service")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "defaultSubscriptionId" -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { result.success(-1); return }
                if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) { result.success(-1); return }
                val voiceSubId = SubscriptionManager.getDefaultVoiceSubscriptionId()
                result.success(voiceSubId)
            }

            "makeRequest" -> {
                val code = call.argument<String>("code")
                val subId = call.argument<Int>("subscriptionId") ?: -1

                if (code.isNullOrBlank()) { result.error("ARG","code vacío",null); return }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { result.error("UNSUPPORTED","API < 26",null); return }

                val permCall = ActivityCompat.checkSelfPermission(appContext, Manifest.permission.CALL_PHONE) ==
                        PackageManager.PERMISSION_GRANTED
                val permState = ActivityCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE) ==
                        PackageManager.PERMISSION_GRANTED
                if (!permCall || !permState) { result.error("PERMISSION","Faltan permisos",null); return }

                val baseTm = appContext.getSystemService(TelephonyManager::class.java)
                val tm = if (subId >= 0) baseTm.createForSubscriptionId(subId) else baseTm

                tm.sendUssdRequest(
                    code,
                    object : TelephonyManager.UssdResponseCallback() {
                        override fun onReceiveUssdResponse(
                            telephonyManager: TelephonyManager,
                            request: String,
                            response: CharSequence
                        ) { result.success(response.toString()) }

                        override fun onReceiveUssdResponseFailed(
                            telephonyManager: TelephonyManager,
                            request: String,
                            failureCode: Int
                        ) { result.error("USSD_FAILED","code=$failureCode",null) }
                    },
                    Handler(Looper.getMainLooper())
                )
            }

            else -> result.notImplemented()
        }
    }
}
