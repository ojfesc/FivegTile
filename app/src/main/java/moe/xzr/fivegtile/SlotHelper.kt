package moe.xzr.fivegtile

import android.telephony.SubscriptionManager

/**
 * subscriptionId 与物理卡槽的映射。
 *
 * cmd phone 的 -s 参数是 SLOT_ID，不是 subscriptionId；
 * 必须通过 SubscriptionManager.getSlotIndex(subId) 转换，
 * 不能用 subId % 2 之类的推断。
 *
 * 该方法在 app 进程和 Shizuku UserService 进程中均可调用：
 * 普通 app 需要 READ_PHONE_STATE，shell/root 身份自带该权限。
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
