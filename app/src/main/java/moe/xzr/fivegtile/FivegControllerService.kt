package moe.xzr.fivegtile

import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import java.util.concurrent.TimeUnit

/**
 * Root 后端的 5G 控制服务。
 * 同样通过 cmd phone 命令操作，确保与 Shizuku 后端逻辑完全一致。
 */
class FivegControllerService : RootService() {

    companion object {
        private const val TAG = "FivegControllerService"
        private const val COMMAND_TIMEOUT_SECONDS = 5L
    }

    override fun onBind(intent: Intent) = object : IFivegController.Stub() {

        override fun getBackendType(): Int = 1

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
            // RootService 进程本身拥有 root 身份，直接执行命令即可
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
}
