package de.simon.dankelmann.bluetoothlespam.Helpers

import android.content.Context
import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.Services.LegacyAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Services.ModernAdvertisementService

class BluetoothHelpers {
    companion object {
        fun supportsBluetooth5(): Boolean {
            var bluetoothAdapter = AppContext.getContext().bluetoothAdapter()
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter!!.isLe2MPhySupported
                    && bluetoothAdapter!!.isLeCodedPhySupported
                    && bluetoothAdapter!!.isLeExtendedAdvertisingSupported
                    && bluetoothAdapter!!.isLePeriodicAdvertisingSupported
                ) {
                    return true
                }
            }
            return false
        }

        fun getAdvertisementService(context: Context): IAdvertisementService {
            var useLegacyAdvertisementService = true

            // Get from Settings, if present
            val preferences = PreferenceManager.getDefaultSharedPreferences(context).all
            val prefKey =
                context.resources.getString(R.string.preference_key_use_legacy_advertising)
            preferences.forEach {
                if (it.key == prefKey) {
                    useLegacyAdvertisementService = it.value as Boolean
                }
            }

            return when (useLegacyAdvertisementService) {
                true -> LegacyAdvertisementService(context)
                else -> {
                    ModernAdvertisementService(context)
                }
            }
        }

        fun getBluetoothLeScanService(context: Context): IBluetoothLeScanService {
            return BluetoothLeScanService(context)
        }
    }
}