package moe.xzr.fivegtile

import android.telephony.SubscriptionManager

/**
 * 将订阅 ID (subId) 映射为物理卡槽 (slotId)。
 */
object SlotHelper {

    fun slotForSubId(subId: Int): Int {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return -1
        return try {
            val slot = SubscriptionManager.getSlotIndex(subId)
            if (slot == SubscriptionManager.INVALID_SIM_SLOT_INDEX) -1 else slot
        } catch (_: Exception) {
            -1
        }
    }
}
