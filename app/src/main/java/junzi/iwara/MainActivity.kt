package junzi.iwara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import junzi.iwara.app.IwaraAppController
import junzi.iwara.ui.IwaraTheme

class MainActivity : ComponentActivity() {
    private lateinit var controller: IwaraAppController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = IwaraAppController(applicationContext)
        setContent {
            IwaraTheme {
                IwaraApp(controller)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.dispose()
    }
}

