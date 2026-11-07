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

        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        val slotId = try {
            SubscriptionManager.getSlotIndex(subId)
        } catch (e: Exception) {
            SubscriptionManager.INVALID_SIM_SLOT_INDEX
        }

        if (!SubscriptionManager.isValidSubscriptionId(subId) || 
            slotId == SubscriptionManager.INVALID_SIM_SLOT_INDEX
        ) {
            setUnavailable()
            return@runWithController
        }

        val checkResult = it.compatibilityCheckWithMessage(subId, slotId)

        qsTile.state = when {
            checkResult != "OK" -> Tile.STATE_UNAVAILABLE
            it.getFivegEnabled(subId, slotId) -> Tile.STATE_ACTIVE
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
            val slotId = try {
                SubscriptionManager.getSlotIndex(subId)
            } catch (e: Exception) {
                SubscriptionManager.INVALID_SIM_SLOT_INDEX
            }

            if (!SubscriptionManager.isValidSubscriptionId(subId) || 
                slotId == SubscriptionManager.INVALID_SIM_SLOT_INDEX
            ) {
                setUnavailable()
                return@runWithController
            }

            when (qsTile.state) {
                Tile.STATE_INACTIVE -> it?.setFivegEnabled(subId, slotId, true)
                Tile.STATE_ACTIVE -> it?.setFivegEnabled(subId, slotId, false)
            }
            refreshState()
        }
    }
}
