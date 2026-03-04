package com.geovpn.poc

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VPN_CORE", "Service started command received")

        if (intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        Log.d("VPN_CORE", "Building VPN Interface...")

        try {
            val builder = Builder()
                .setSession("GeoVPN PoC")
                .setMtu(1500)
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")

            vpnInterface = builder.establish()

            Log.d("VPN_CORE", "VPN Interface established! FD: ${vpnInterface?.fd}")
            // LibXray.runXray(...)

        } catch (e: Exception) {
            Log.e("VPN_CORE", "Error starting VPN: ${e.message}")
            stopSelf()
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.d("VPN_CORE", "VPN stopped")
            // LibXray.stopXray()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

}