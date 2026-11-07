package moe.xzr.fivegtile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import java.lang.reflect.Method

/**
 * Root 后端的 5G 控制服务。
 *
 * TelephonyManager 在 RootService 进程中无法正确初始化，
 * 因此直接通过反射调用 telephony 服务，避免 AIDL 签名在 Android 17+ 上的不匹配。
 */
class FivegControllerService : RootService() {
    companion object {
        private const val TAG = "FivegControllerService"

        private val telephonyService by lazy {
            ServiceManager.getService(Context.TELEPHONY_SERVICE)
        }

        private val iTelephony by lazy {
            val asInterfaceMethod = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                .getMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.invoke(null, telephonyService)
        }

        private val reasonUser by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("ALLOWED_NETWORK_TYPES_REASON_USER")
                .getInt(null)
        }

        private val typeNr by lazy {
            Class.forName("android.telephony.TelephonyManager")
                .getDeclaredField("NETWORK_TYPE_BITMASK_NR")
                .getLong(null)
        }

        @delegate:SuppressLint("PrivateApi", "BlockedPrivateApi")
        private val modeLte by lazy {
            Class.forName("com.android.internal.telephony.RILConstants")
                .getDeclaredField("NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA")
                .getInt(null)
        }

        @delegate:SuppressLint("PrivateApi", "BlockedPrivateApi")
        private val modeNr by lazy {
            Class.forName("com.android.internal.telephony.RILConstants")
                .getDeclaredField("NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA")
                .getInt(null)
        }

        private var getAllowedNetworkTypesMethod: Method? = null
        private var setAllowedNetworkTypesMethod: Method? = null
        private var getPreferredNetworkTypeMethod: Method? = null
        private var setPreferredNetworkTypeMethod: Method? = null

        private fun initReflectionMethods() {
            if (getAllowedNetworkTypesMethod != null) return

            try {
                val iTelephonyClass = iTelephony.javaClass
                Log.d(TAG, "iTelephony class: ${iTelephonyClass.name}")

                val methods = iTelephonyClass.methods

                getAllowedNetworkTypesMethod = methods.find {
                    it.name == "getAllowedNetworkTypesForReason" && it.parameterTypes.size == 2
                }

                // Android 17+ 签名变为 (int, int, long, String)：boolean
                // 旧版本是 (int, int, long)
                setAllowedNetworkTypesMethod = methods.find {
                    it.name == "setAllowedNetworkTypesForReason" &&
                            it.parameterTypes.size >= 3 &&
                            it.parameterTypes[0] == Int::class.javaPrimitiveType &&
                            it.parameterTypes[1] == Int::class.javaPrimitiveType &&
                            it.parameterTypes[2] == Long::class.javaPrimitiveType
                }

                getPreferredNetworkTypeMethod = methods.find {
                    it.name == "getPreferredNetworkType" && it.parameterTypes.size == 1
                }

                setPreferredNetworkTypeMethod = methods.find {
                    it.name == "setPreferredNetworkType" && it.parameterTypes.size == 2
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to init reflection methods", e)
            }
        }
    }

    override fun onBind(intent: Intent) = object : IFivegController.Stub() {

        override fun getBackendType(): Int = 1

        override fun compatibilityCheck(subId: Int): Boolean {
            return try {
                initReflectionMethods()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val getMethod = getAllowedNetworkTypesMethod ?: return false
                    val setMethod = setAllowedNetworkTypesMethod ?: return false
                    val currentTypes = getMethod.invoke(iTelephony, subId, reasonUser) as Long
                    if (setMethod.parameterTypes.size >= 4) {
                        setMethod.invoke(
                            iTelephony, subId, reasonUser, currentTypes, "moe.xzr.fivegtile"
                        )
                    } else {
                        setMethod.invoke(iTelephony, subId, reasonUser, currentTypes)
                    }
                    true
                } else {
                    modeLte
                    modeNr
                    val getMethod = getPreferredNetworkTypeMethod ?: return false
                    val setMethod = setPreferredNetworkTypeMethod ?: return false
                    val currentType = getMethod.invoke(iTelephony, subId) as Int
                    setMethod.invoke(iTelephony, subId, currentType)
                    true
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Compatibility check failed", e)
                false
            }
        }

        override fun getFivegEnabled(subId: Int): Boolean {
            return try {
                initReflectionMethods()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val getMethod = getAllowedNetworkTypesMethod ?: return false
                    val types = getMethod.invoke(iTelephony, subId, reasonUser) as Long
                    types and typeNr != 0L
                } else {
                    val getMethod = getPreferredNetworkTypeMethod ?: return false
                    val currentType = getMethod.invoke(iTelephony, subId) as Int
                    currentType == modeNr
                }
            } catch (e: Throwable) {
                Log.e(TAG, "getFivegEnabled failed", e)
                false
            }
        }

        override fun setFivegEnabled(subId: Int, enabled: Boolean) {
            try {
                initReflectionMethods()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val getMethod = getAllowedNetworkTypesMethod ?: return
                    val setMethod = setAllowedNetworkTypesMethod ?: return
                    var curTypes = getMethod.invoke(iTelephony, subId, reasonUser) as Long
                    curTypes = if (enabled) curTypes or typeNr else curTypes and typeNr.inv()

                    if (setMethod.parameterTypes.size >= 4) {
                        setMethod.invoke(
                            iTelephony, subId, reasonUser, curTypes, "moe.xzr.fivegtile"
                        )
                    } else {
                        setMethod.invoke(iTelephony, subId, reasonUser, curTypes)
                    }
                } else {
                    val setMethod = setPreferredNetworkTypeMethod ?: return
                    val targetMode = if (enabled) modeNr else modeLte
                    setMethod.invoke(iTelephony, subId, targetMode)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "setFivegEnabled failed", e)
            }
        }
    }
}
