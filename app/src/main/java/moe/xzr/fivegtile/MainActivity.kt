package moe.xzr.fivegtile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager

class FivegTileService : TileService() {

    private fun runWithController(what: (IFivegController?) -> Unit) {
        BackendController.bind(this) { controller -> what(controller) }
    }

    private fun refreshState() = runWithController {
        if (it == null) {
            setUnavailable()
            return@runWithController
        }

        // 每次刷新重新读取默认数据订阅，
        // 用户在系统设置中切换默认数据卡后 subId 会变化。
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            setUnavailable()
            return@runWithController
        }

        // 使用新接口 compatibilityCheckWithMessage，如果返回 OK 说明兼容
        val checkResult = it.compatibilityCheckWithMessage(subId)

        qsTile.state = when {
            checkResult != "OK" -> Tile.STATE_UNAVAILABLE
            it.getFivegEnabled(subId) -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        qsTile.updateTile()
    }

    private fun setUnavailable() {
        qsTile.state = Tile.STATE_UNAVAILABLE
        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshState()
    }

    override fun onClick() {
        super.onClick()
        runWithController {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                setUnavailable()
                return@runWithController
            }
            when (qsTile.state) {
                Tile.STATE_INACTIVE -> it?.setFivegEnabled(subId, true)
                Tile.STATE_ACTIVE -> it?.setFivegEnabled(subId, false)
            }
            refreshState()
        }
    }
}
