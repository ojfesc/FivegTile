package moe.xzr.fivegtile

import android.content.Context
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Shizuku / Sui 后端的 5G 控制服务。
 */
class ShizukuControllerService : IFivegController.Stub {

    companion object {
        private const val TAG = "ShizukuControllerService"
        private const val COMMAND_TIMEOUT_SECONDS = 5L
    }

    constructor() : super()
    constructor(context: Context) : super()

    override fun getBackendType(): Int = 2

    override fun compatibilityCheckWithMessage(subId: Int, slotId: Int): String {
        return try {
            if (slotId < 0) throw IllegalArgumentException("无效的卡槽，slotId=$slotId")
            val output = exec(NetworkCommands.get(slotId))
            NetworkCommands.parseMask(output)
            "OK"
        } catch (e: Exception) {
            Log.e(TAG, "compatibilityCheck failed", e)
            e.message ?: e.javaClass.simpleName
        }
    }

    override fun getFivegEnabled(subId: Int, slotId: Int): Boolean {
        return try {
            if (slotId < 0) return false
            NetworkCommands.hasNr(exec(NetworkCommands.get(slotId)))
        } catch (e: Exception) {
            Log.e(TAG, "getFivegEnabled failed", e)
            false
        }
    }

    override fun setFivegEnabled(subId: Int, slotId: Int, enabled: Boolean) {
        try {
            if (slotId < 0) throw IllegalArgumentException("无效的卡槽，slotId=$slotId")
            val mask = NetworkCommands.parseMask(exec(NetworkCommands.get(slotId)))
            val target = if (enabled) mask or NetworkCommands.NR_BIT
            else mask and NetworkCommands.NR_BIT.inv()
            if (target == 0L) {
                throw IllegalStateException("网络类型为空，请先在系统设置选择自动网络模式")
            }
            exec(NetworkCommands.set(slotId, target))
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
