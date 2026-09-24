package samples.android.status

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MainActivity : ComponentActivity() {
    private val providers = App.providers
    private val logger = providers.loggers.create("[Main]")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context: Context = this
        LinearLayout(context).also { root ->
            root.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            root.orientation = LinearLayout.VERTICAL
            root.gravity = Gravity.CENTER_VERTICAL
            val deviceOwnerTitle = TextView(context).also { view ->
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                root.addView(view)
            }
            val deviceOwnerButton = Button(context).also { view ->
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                view.text = "remove admin"
                view.setOnClickListener { _ ->
                    providers.admins.update(isDeviceOwner = false)
                }
                root.addView(view)
            }
            lifecycleScope.launch {
                withContext(providers.contexts.default) {
                    providers.admins.owners.collect { isDeviceOwner ->
                        logger.debug("device:owner: $isDeviceOwner")
                        withContext(providers.contexts.main) {
                            deviceOwnerTitle.text = "device:owner: $isDeviceOwner"
                            deviceOwnerButton.isEnabled = isDeviceOwner
                        }
                    }
                }
            }
            setContentView(root)
        }
    }
}
