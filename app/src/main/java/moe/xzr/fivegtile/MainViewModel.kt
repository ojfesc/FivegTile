package moe.xzr.fivegtile

import android.app.Application
import android.telephony.SubscriptionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        var lastErrorMessage: String? = null
    }

    enum class CompatibilityState {
        PENDING,
        ROOT_DENIED,
        NOT_COMPATIBLE,
        MISSING_PERMISSION,
        COMPATIBLE,
    }

    @Composable
    fun getCompatibilityState(): CompatibilityState {
        val ret by produceState(initialValue = CompatibilityState.PENDING) {
            val context = getApplication<Application>()

            val backend = withContext(Dispatchers.IO) {
                try {
                    BackendController.detectBackend(context)
                } catch (_: Throwable) {
                    BackendController.Backend.NONE
                }
            }

            if (backend == BackendController.Backend.NONE) {
                value = CompatibilityState.ROOT_DENIED
                return@produceState
            }

            BackendController.bind(context) { controller ->
                if (controller == null) {
                    value = CompatibilityState.NOT_COMPATIBLE
                } else {
                    val subId = SubscriptionManager.getDefaultDataSubscriptionId()
                    if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                        value = CompatibilityState.MISSING_PERMISSION
                        return@bind
                    }
                    
                    try {
                        val msg = controller.compatibilityCheckWithMessage(subId)
                        if (msg == "OK") {
                            value = CompatibilityState.COMPATIBLE
                        } else {
                            lastErrorMessage = msg
                            value = CompatibilityState.NOT_COMPATIBLE
                        }
                    } catch (e: Throwable) {
                        lastErrorMessage = e.message ?: e.javaClass.simpleName
                        value = CompatibilityState.NOT_COMPATIBLE
                    }
                }
            }
        }
        return ret
    }
}
