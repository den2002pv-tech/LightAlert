package app.lightalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lightalert.presentation.home.HomeScreen
import app.lightalert.presentation.home.HomeViewModel
import app.lightalert.presentation.theme.LightAlertTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LightAlertTheme {
                val homeViewModel: HomeViewModel = viewModel()
                val uiState = homeViewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(state = uiState.value)
            }
        }
    }
}
