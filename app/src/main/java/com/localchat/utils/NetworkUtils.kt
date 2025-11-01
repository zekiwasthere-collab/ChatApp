package com.localchat.utils

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Get device's local IP address from WiFi connection
     * Returns IP in dotted decimal format (e.g., "192.168.43.1")
     * Returns null if not connected to WiFi
     */
    fun getLocalIpAddress(context: Context): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipInt = wifiInfo.ipAddress

            if (ipInt == 0) {
                // WiFi not connected, try to get IP from network interfaces
                return getIpFromNetworkInterface()
            }

            // Convert integer IP to dotted decimal format
            return String.format(
                "%d.%d.%d.%d",
                (ipInt and 0xff),
                (ipInt shr 8 and 0xff),
                (ipInt shr 16 and 0xff),
                (ipInt shr 24 and 0xff)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Get IP address from network interfaces
     * Used as fallback when WifiManager doesn't return IP
     */
    private fun getIpFromNetworkInterface(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Validate IP:port format
     * Checks format: "xxx.xxx.xxx.xxx:pppp"
     * Validates IP octets (0-255) and port (1-65535)
     * Returns true if valid, false otherwise
     */
    fun isValidIpAddress(address: String): Boolean {
        try {
            // Check for colon separator
            if (!address.contains(":")) {
                return false
            }

            val parts = address.split(":")
            if (parts.size != 2) {
                return false
            }

            val ip = parts[0]
            val portStr = parts[1]

            // Validate IP format
            val ipParts = ip.split(".")
            if (ipParts.size != 4) {
                return false
            }

            // Validate each IP octet (0-255)
            for (octet in ipParts) {
                val num = octet.toIntOrNull() ?: return false
                if (num < 0 || num > 255) {
                    return false
                }
            }

            // Validate port (1-65535)
            val port = portStr.toIntOrNull() ?: return false
            if (port < 1 || port > 65535) {
                return false
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }
}
