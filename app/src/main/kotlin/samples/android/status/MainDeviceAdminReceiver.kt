package samples.android.status

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

internal class MainDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        // todo
    }
}
