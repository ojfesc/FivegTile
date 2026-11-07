package moe.xzr.fivegtile

import java.util.Locale

/**
 * 生成 cmd phone 命令并解析系统返回的网络类型列表。
 * 只修改 NR bit，保留其他制式。
 */
object NetworkCommands {

    const val NR_BIT = 1L shl 19

    fun get(slot: Int): String {
        require(slot >= 0) { "invalid slot: $slot" }
        return "cmd phone get-allowed-network-types-for-users -s $slot"
    }

    fun set(slot: Int, mask: Long): String {
        require(slot >= 0) { "invalid slot: $slot" }
        require(mask != 0L) { "network mask empty" }
        return "cmd phone set-allowed-network-types-for-users -s $slot ${Long.toBinaryString(mask)}"
    }

    fun parseMask(output: String): Long {
        if (output.isBlank()) {
            throw IllegalArgumentException("系统未返回网络类型")
        }
        var mask = 0L
        for (part in output.trim().split("|")) {
            val bit = bitForType(part.trim().uppercase(Locale.ROOT))
                ?: throw IllegalArgumentException("无法识别网络类型：$output")
            mask = mask or (1L shl bit)
        }
        return mask
    }

    fun hasNr(output: String): Boolean = (parseMask(output) and NR_BIT) != 0L

    private fun bitForType(type: String): Int? = when (type) {
        "GPRS" -> 0
        "EDGE" -> 1
        "UMTS" -> 2
        "CDMA" -> 3
        "CDMA - EVDO REV. 0", "EVDO_0" -> 4
        "CDMA - EVDO REV. A", "EVDO_A" -> 5
        "CDMA - 1XRTT", "1XRTT" -> 6
        "HSDPA" -> 7
        "HSUPA" -> 8
        "HSPA" -> 9
        "IDEN" -> 10
        "CDMA - EVDO REV. B", "EVDO_B" -> 11
        "LTE" -> 12
        "CDMA - EHRPD", "EHRPD" -> 13
        "HSPA+", "HSPAP" -> 14
        "GSM" -> 15
        "TD_SCDMA" -> 16
        "IWLAN" -> 17
        "LTE_CA" -> 18
        "NR" -> 19
        else -> null
    }
}
