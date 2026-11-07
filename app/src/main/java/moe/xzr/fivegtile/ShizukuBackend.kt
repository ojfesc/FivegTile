package moe.xzr.fivegtile

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * 封装 Shizuku / Sui 的可用性判断与权限请求。
 * Sui 是 Shizuku 的 root 后端，客户端 API 一致，无需单独分支。
 */
object ShizukuBackend {

    const val REQUEST_CODE = 1001

    /** Shizuku binder 存活且已授权。 */
    fun isReady(): Boolean = try {
        Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    /** Shizuku binder 是否存活，用于区分“未运行”和“未授权”。 */
    fun isBinderAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    /**
     * 请求 Shizuku 权限。调用方需要注册
     * Shizuku.OnRequestPermissionResultListener 才能收到结果。
     */
    fun requestPermission() {
        try {
            if (!Shizuku.pingBinder()) return
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Throwable) {
            // Shizuku 未运行或版本不兼容时静默失败。
        }
    }
}
