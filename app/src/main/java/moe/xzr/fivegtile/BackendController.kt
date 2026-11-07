package moe.xzr.fivegtile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

/**
 * 客户端统一入口。
 */
object BackendController {

    private const val TAG = "BackendController"

    enum class Backend { NONE, ROOT, SHIZUKU }

    @Volatile
    private var controller: IFivegController? = null

    @Volatile
    private var backend: Backend = Backend.NONE

    private var rootConnection: ServiceConnection? = null
    private var shizukuArgs: Shizuku.UserServiceArgs? = null
    private var shizukuConnection: ServiceConnection? = null

    private var binding = false
    private val pending = mutableListOf<(IFivegController?) -> Unit>()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun currentBackend(): Backend = backend

    fun detectBackend(context: Context): Backend = when {
        ShizukuBackend.isReady() -> Backend.SHIZUKU
        Utils.isRootGranted() -> Backend.ROOT
        else -> Backend.NONE
    }

    fun bind(context: Context, callback: (IFivegController?) -> Unit) {
        val existing = controller
        if (existing != null) {
            callback(existing)
            return
        }

        synchronized(this) {
            pending.add(callback)
            if (binding) return
            binding = true
        }

        val appContext = context.applicationContext
        executor.execute {
            val target = try {
                detectBackend(appContext)
            } catch (e: Throwable) {
                Log.e(TAG, "detectBackend failed", e)
                Backend.NONE
            }
            backend = target
            when (target) {
                Backend.SHIZUKU -> bindShizuku(appContext)
                Backend.ROOT -> bindRoot(appContext)
                Backend.NONE -> finishBind(null)
            }
        }
    }

    private fun bindShizuku(context: Context) {
        try {
            val args = Shizuku.UserServiceArgs(
                ComponentName(context, ShizukuControllerService::class.java)
            )
                .daemon(true)
                .processNameSuffix("fiveg")
                .tag("fiveg-shizuku")
                .version(1)

            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val c = IFivegController.Stub.asInterface(service)
                    finishBind(c)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    synchronized(this@BackendController) {
                        controller = null
                    }
                    Log.w(TAG, "Shizuku UserService disconnected")
                }
            }

            shizukuArgs = args
            shizukuConnection = connection
            Shizuku.bindUserService(args, connection)
        } catch (e: Throwable) {
            Log.e(TAG, "bindShizuku failed", e)
            finishBind(null)
        }
    }

    private fun bindRoot(context: Context) {
        try {
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val c = IFivegController.Stub.asInterface(service)
                    finishBind(c)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    synchronized(this@BackendController) {
                        controller = null
                    }
                    Log.w(TAG, "RootService disconnected")
                }
            }
            rootConnection = connection
            RootService.bind(
                Intent(context, FivegControllerService::class.java),
                connection
            )
        } catch (e: Throwable) {
            Log.e(TAG, "bindRoot failed", e)
            finishBind(null)
        }
    }

    private fun finishBind(result: IFivegController?) {
        val callbacks: List<(IFivegController?) -> Unit>
        synchronized(this) {
            binding = false
            callbacks = pending.toList()
            pending.clear()
            if (result != null) controller = result
        }
        mainHandler.post {
            callbacks.forEach { it(result) }
        }
    }
}
