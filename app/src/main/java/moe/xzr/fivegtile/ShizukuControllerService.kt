package moe.xzr.fivegtile

import android.content.Context
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Shizuku / Sui 后端的 5G 控制服务。
 * 注意：类头部保留 () 隐式调用父类无参构造，带 Context 的构造必须委托给 this()。
 */
class ShizukuControllerService : IFivegController.Stub() {

    companion object {
        private const val TAG = "ShizukuControllerService"
        private const val COMMAND_TIMEOUT_SECONDS = 5L
    }

    constructor(context: Context) : this()

    override fun getBackendType(): Int = 2

    override fun compatibilityCheckWithMessage(subId: Int): String {
        return try {
            val output = exec(NetworkCommands.get(subId))
            NetworkCommands.parseMask(output)
            "OK"
        } catch (e: Exception) {
            Log.e(TAG, "compatibilityCheck failed", e)
            e.message ?: e.javaClass.simpleName
        }
    }

    override fun getFivegEnabled(subId: Int): Boolean {
        return try {
            NetworkCommands.hasNr(exec(NetworkCommands.get(subId)))
        } catch (e: Exception) {
            Log.e(TAG, "getFivegEnabled failed", e)
            false
        }
    }

    override fun setFivegEnabled(subId: Int, enabled: Boolean) {
        try {
            val mask = NetworkCommands.parseMask(exec(NetworkCommands.get(subId)))
            val target = if (enabled) mask or NetworkCommands.NR_BIT
            else mask and NetworkCommands.NR_BIT.inv()
            if (target == 0L) {
                throw IllegalStateException("网络类型为空，请先在系统设置选择自动网络模式")
            }
            exec(NetworkCommands.set(subId, target))
        } catch (e: Exception) {
            Log.e(TAG, "setFivegEnabled failed", e)
        }
    }

    private fun exec(command: String): String {
        val process = ProcessBuilder("/system/bin/sh", "-c", command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()

        if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            throw IllegalStateException("系统命令超时")
        }
        
        val exitCode = process.exitValue()
        process.destroy()

        if (exitCode != 0) {
            throw IllegalStateException("exit=$exitCode $output")
        }
        return output
    }
}
