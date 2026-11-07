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

    enum class CompatibilityState {
        PENDING,
        ROOT_DENIED,
        NOT_COMPATIBLE,
        COMPATIBLE,
    }

    @Composable
    fun getCompatibilityState(): CompatibilityState {
        val ret by produceState(initialValue = CompatibilityState.PENDING) {
            val context = getApplication<Application>()

            // detectBackend 内部可能调用 Shell.getShell()，放到 IO 线程。
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
                    return@bind
                }
                val subId = SubscriptionManager.getDefaultDataSubscriptionId()
                val compatible = try {
                    SubscriptionManager.isValidSubscriptionId(subId) &&
                            controller.compatibilityCheck(subId)
                } catch (_: Throwable) {
                    false
                }
                value = if (compatible) {
                    CompatibilityState.COMPATIBLE
                } else {
                    CompatibilityState.NOT_COMPATIBLE
                }
            }
        }
        return ret
    }
}
