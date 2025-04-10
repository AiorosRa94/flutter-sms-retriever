package me.arsam.sms_retriever

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class AppSignatureHelper(context: Context) : ContextWrapper(context) {

    companion object {
        val TAG = AppSignatureHelper::class.java.simpleName
        private const val HASH_TYPE = "SHA-256"
        private const val NUM_HASHED_BYTES = 9
        private const val NUM_BASE64_CHAR = 11
    }

fun getAppSignatures(): ArrayList<String> {
    val appCodes = ArrayList<String>()

    try {
        val packageName = packageName
        val packageManager = packageManager

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val rawSignatures = packageManager
                .getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners
            rawSignatures?.filterNotNull()?.toTypedArray() ?: emptyArray()
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            ).signatures?.filterNotNull()?.toTypedArray() ?: emptyArray()
        }

        signatures
            .mapNotNull { hash(packageName, it.toCharsString()) }
            .mapTo(appCodes) { it }

    } catch (e: PackageManager.NameNotFoundException) {
        Log.e(TAG, "Unable to find package to obtain hash.", e)
    }

    return appCodes
}





    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun hash(packageName: String, signature: String): String? {
        val appInfo = "$packageName $signature"
        return try {
            val messageDigest = MessageDigest.getInstance(HASH_TYPE)
            messageDigest.update(appInfo.toByteArray(StandardCharsets.UTF_8))
            var hashSignature = messageDigest.digest()

            // truncated into NUM_HASHED_BYTES
            hashSignature = Arrays.copyOfRange(hashSignature, 0, NUM_HASHED_BYTES)
            // encode into Base64
            var base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING or Base64.NO_WRAP)
            base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR)

            base64Hash
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "hash:NoSuchAlgorithm", e)
            null
        }
    }
}
