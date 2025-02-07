package mock

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.HardwareDevice.Manufacturer

class MockHardwareDevice(var name: String = "", manufacturer: Manufacturer = Manufacturer.Other) : HardwareDevice {
    private var mutableManufacturer = manufacturer

    override fun getManufacturer(): Manufacturer = mutableManufacturer

    fun setManufacturer(manufacturer: Manufacturer) {
        mutableManufacturer = manufacturer
    }

    override fun getDeviceName(): String = name

    override fun getConnectionInfo(): String = ""

    override fun getVersion(): Int = -1

    override fun resetDeviceConfigurationForOpMode() {}

    override fun close() {}
}