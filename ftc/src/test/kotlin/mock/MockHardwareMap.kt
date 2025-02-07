package mock

import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier
import com.qualcomm.robotcore.hardware.HardwareMap

class MockHardwareMap(notifier: OpModeManagerNotifier) : HardwareMap(mockApplication, notifier) {
    override fun <T : Any?> tryGet(classOrInterface: Class<out T>, deviceName: String): T? {
        val name = deviceName.trim { it <= ' ' }
        val list = allDevicesMap[name] ?: return null
        var result: T? = null

        for (device in list) {
            if (classOrInterface.isInstance(device)) {
                //Not initializing imu here
                result = classOrInterface.cast(device)
                break
            }
        }

        //Not doing imu check

        //Not doing driver check

        return result
    }
}