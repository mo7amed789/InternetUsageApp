package com.example.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class LocalDataSaverVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.example.datapulse.VPN_START"
        const val ACTION_STOP = "com.example.datapulse.VPN_STOP"

        var isVpnActive = false
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                stopSelf()
            }
            else -> {
                scope.launch {
                    startLocalVpn()
                }
            }
        }
        return START_STICKY
    }

    private suspend fun startLocalVpn() {
        try {
            stopVpn()

            val builder = Builder()
                .setSession("DataPulse Firewall")
                .addAddress("10.1.10.1", 24)

            // Read blocked packages from local DB
            val db = AppDatabase.getInstance(this)
            val rules = db.appBlockDao().getAllRules().firstOrNull() ?: emptyList()

            val blockedPackages = rules.filter { it.isBlockedMobile || it.isBlockedWifi }.map { it.packageName }

            if (blockedPackages.isNotEmpty()) {
                // Route only the blocked packages to the local blackhole tun interface
                for (pkg in blockedPackages) {
                    try {
                        builder.addAllowedApplication(pkg)
                    } catch (e: Exception) {
                        Log.w("DataSaverVPN", "Could not route package: $pkg")
                    }
                }
            } else {
                // Default local protective interface
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    // Ignore
                }
            }

            vpnInterface = builder.establish()
            isVpnActive = true
            Log.d("DataSaverVPN", "Local Data Saver VPN successfully established.")
        } catch (e: Exception) {
            Log.e("DataSaverVPN", "Failed to establish VPN: ${e.message}")
            isVpnActive = false
            stopSelf()
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // Ignore
        }
        vpnInterface = null
        isVpnActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
