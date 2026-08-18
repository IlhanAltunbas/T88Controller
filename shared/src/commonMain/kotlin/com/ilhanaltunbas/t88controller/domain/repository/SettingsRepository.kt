package com.ilhanaltunbas.t88controller.domain.repository

import com.ilhanaltunbas.t88controller.domain.model.SavedDevice

interface SettingsRepository {
    fun saveLastIp(ip: String)
    fun getLastIp(): String
    fun saveLastPort(port: Int)
    fun getLastPort(): Int
    
    // Çoklu Cihaz Yönetimi
    fun saveDevice(device: SavedDevice)
    fun getSavedDevices(): List<SavedDevice>
    fun deleteDevice(ip: String, port: Int)

    // Kanal İsimlerini Kaydetme
    fun saveChannelName(deviceIp: String, id: Int, isInput: Boolean, name: String)
    fun getChannelName(deviceIp: String, id: Int, isInput: Boolean, defaultName: String): String
}