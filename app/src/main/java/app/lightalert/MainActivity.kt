package app.lightalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lightalert.presentation.home.HomeScreen
import app.lightalert.presentation.home.HomeViewModel
import app.lightalert.presentation.home.HomeViewModelFactory
import app.lightalert.presentation.telegram.TelegramAuthScreen
import app.lightalert.presentation.telegram.TelegramAuthViewModel
import app.lightalert.presentation.telegram.TelegramAuthViewModelFactory
import app.lightalert.presentation.telegram.TelegramChannelScreen
import app.lightalert.presentation.telegram.TelegramChannelViewModel
import app.lightalert.presentation.telegram.TelegramChannelViewModelFactory
import app.lightalert.presentation.theme.LightAlertTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LightAlertApplication

        setContent {
            LightAlertTheme {
                var showTelegramConnection by rememberSaveable { mutableStateOf(false) }
                var showChannelSelection by rememberSaveable { mutableStateOf(false) }

                if (showChannelSelection) {
                    val channelViewModel: TelegramChannelViewModel = viewModel(
                        factory = TelegramChannelViewModelFactory(
                            app.telegramClient,
                            app.telegramChatSelectionStore,
                        ),
                    )
                    val channelState = channelViewModel.uiState.collectAsStateWithLifecycle()
                    TelegramChannelScreen(
                        state = channelState.value,
                        onBack = { showChannelSelection = false; showTelegramConnection = true },
                        onSearchChange = channelViewModel::setSearchQuery,
                        onToggle = channelViewModel::toggleChat,
                        onRefresh = channelViewModel::refresh,
                        onSave = channelViewModel::saveSelection,
                    )
                } else if (showTelegramConnection) {
                    val telegramViewModel: TelegramAuthViewModel = viewModel(
                        factory = TelegramAuthViewModelFactory(app.telegramClient, app.telegramApiConfigStore),
                    )
                    val telegramState = telegramViewModel.uiState.collectAsStateWithLifecycle()
                    TelegramAuthScreen(
                        state = telegramState.value,
                        onBack = { showTelegramConnection = false },
                        onSaveApiConfig = telegramViewModel::saveApiConfig,
                        onSubmitPhone = telegramViewModel::submitPhoneNumber,
                        onSubmitCode = telegramViewModel::submitCode,
                        onSubmitPassword = telegramViewModel::submitPassword,
                        onRetry = telegramViewModel::retryConnection,
                        onSelectChannels = { showChannelSelection = true },
                    )
                } else {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModelFactory(app.scheduleRepository),
                    )
                    val homeState = homeViewModel.uiState.collectAsStateWithLifecycle()
                    HomeScreen(
                        state = homeState.value,
                        onConnectTelegram = { showTelegramConnection = true },
                    )
                }
            }
        }
    }
}
