package com.ilhanaltunbas.t88controller.data.repository

import com.ilhanaltunbas.t88controller.domain.model.SavedDevice
import com.ilhanaltunbas.t88controller.domain.repository.SettingsRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepositoryImpl : SettingsRepository {
    private val settings: Settings = Settings()
    
    private val KEY_LAST_IP = "last_ip"
    private val KEY_LAST_PORT = "last_port"
    private val KEY_SAVED_DEVICES = "saved_devices"
    private val PREFIX_CHANNEL_NAME = "ch_name_"

    override fun saveLastIp(ip: String) {
        settings[KEY_LAST_IP] = ip
    }

    override fun getLastIp(): String = settings.getString(KEY_LAST_IP, "192.168.1.100")

    override fun saveLastPort(port: Int) {
        settings[KEY_LAST_PORT] = port
    }

    override fun getLastPort(): Int = settings.getInt(KEY_LAST_PORT, 5000)

    override fun saveDevice(device: SavedDevice) {
        val currentDevices = getSavedDevices().toMutableList()
        currentDevices.removeAll { it.ip == device.ip && it.port == device.port }
        currentDevices.add(0, device)
        
        try {
            val json = Json.encodeToString(currentDevices)
            settings[KEY_SAVED_DEVICES] = json
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getSavedDevices(): List<SavedDevice> {
        val json = settings.getStringOrNull(KEY_SAVED_DEVICES) ?: return emptyList()
        return try {
            Json.decodeFromString<List<SavedDevice>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun deleteDevice(ip: String, port: Int) {
        val currentDevices = getSavedDevices().toMutableList()
        currentDevices.removeAll { it.ip == ip && it.port == port }
        
        try {
            val json = Json.encodeToString(currentDevices)
            settings[KEY_SAVED_DEVICES] = json
            
            // Cihaza ait tüm kanal isimlerini de temizle (Cleanup)
            for (id in 1..8) {
                settings.remove("$PREFIX_CHANNEL_NAME${ip}_in_$id")
                settings.remove("$PREFIX_CHANNEL_NAME${ip}_out_$id")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun saveChannelName(deviceIp: String, id: Int, isInput: Boolean, name: String) {
        val type = if (isInput) "in" else "out"
        val key = "$PREFIX_CHANNEL_NAME${deviceIp}_${type}_$id"
        settings[key] = name
    }

    override fun getChannelName(deviceIp: String, id: Int, isInput: Boolean, defaultName: String): String {
        val type = if (isInput) "in" else "out"
        val key = "$PREFIX_CHANNEL_NAME${deviceIp}_${type}_$id"
        return settings.getString(key, defaultName)
    }
}