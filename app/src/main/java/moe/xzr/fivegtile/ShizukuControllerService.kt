package moe.xzr.fivegtile

import android.content.Context
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Shizuku / Sui 后端的 5G 控制服务。
 *
 * 作为 Shizuku UserService 运行，进程身份为 shell（Shizuku 通过 adb 启动）
 * 或 root（Sui / Shizuku root 模式），因此可以执行需要 MODIFY_PHONE_STATE 的
 * cmd phone 命令。
 *
 * Shizuku 通过反射实例化该类，要求存在无参构造函数或 Context 构造函数，
 * 且类本身实现 IInterface.Stub。
 */
class ShizukuControllerService : IFivegController.Stub {

    companion object {
        private const val TAG = "ShizukuControllerService"
        private const val COMMAND_TIMEOUT_SECONDS = 5L
    }

    constructor()
    constructor(context: Context)

    override fun getBackendType(): Int = 2

    override fun compatibilityCheck(subId: Int): Boolean {
        return try {
            val slot = SlotHelper.slotForSubId(subId)
            if (slot < 0) return false
            NetworkCommands.parseMask(exec(NetworkCommands.get(slot)))
            true
        } catch (e: Exception) {
            Log.e(TAG, "compatibilityCheck failed", e)
            false
        }
    }

    override fun getFivegEnabled(subId: Int): Boolean {
        return try {
            val slot = SlotHelper.slotForSubId(subId)
            if (slot < 0) return false
            NetworkCommands.hasNr(exec(NetworkCommands.get(slot)))
        } catch (e: Exception) {
            Log.e(TAG, "getFivegEnabled failed", e)
            false
        }
    }

    override fun setFivegEnabled(subId: Int, enabled: Boolean) {
        try {
            val slot = SlotHelper.slotForSubId(subId)
            if (slot < 0) throw IllegalArgumentException("invalid subId: $subId")
            val mask = NetworkCommands.parseMask(exec(NetworkCommands.get(slot)))
            val target = if (enabled) mask or NetworkCommands.NR_BIT
            else mask and NetworkCommands.NR_BIT.inv()
            if (target == 0L) {
                throw IllegalStateException("网络类型为空，请先在系统设置选择自动网络模式")
            }
            exec(NetworkCommands.set(slot, target))
        } catch (e: Exception) {
            Log.e(TAG, "setFivegEnabled failed", e)
        }
    }

    private fun exec(command: String): String {
        val process = ProcessBuilder("/system/bin/sh", "-c", command)
            .redirectErrorStream(true)
            .start()

        val output = try {
            process.inputStream.bufferedReader().use { it.readText() }.trim()
        } finally {
            process.destroy()
        }

        if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            throw IllegalStateException("系统命令超时")
        }
        if (process.exitValue() != 0) {
            throw IllegalStateException("exit=${process.exitValue()} $output")
        }
        return output
    }
}
