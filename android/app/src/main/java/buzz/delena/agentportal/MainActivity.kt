package buzz.delena.agentportal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import buzz.delena.agentportal.nav.AgentPortalNavHost
import buzz.delena.agentportal.theme.AgentPortalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgentPortalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AgentPortalNavHost()
                }
            }
        }
    }
}
