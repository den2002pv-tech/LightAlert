package app.lightalert.presentation.telegram

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import app.lightalert.data.telegram.TelegramAuthState

@Composable
fun TelegramAuthScreen(
    state: TelegramAuthUiState,
    onBack: () -> Unit,
    onSaveApiConfig: (String, String) -> Unit,
    onSubmitPhone: (String) -> Unit,
    onSubmitCode: (String) -> Unit,
    onSubmitPassword: (String) -> Unit,
    onRetry: () -> Unit,
    onSelectChannels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var apiId by remember { mutableStateOf("") }
    var apiHash by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(modifier = modifier) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TextButton(onClick = onBack) {
                Text("Назад")
            }

            Text(
                text = "Подключение Telegram",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = state.authState.toDisplayText(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (state.authState) {
                TelegramAuthState.NOT_CONFIGURED -> {
                    Text(
                        text = "Получите API ID и API Hash на my.telegram.org/apps. " +
                            "Введите их здесь один раз — приложение сохранит параметры в зашифрованном виде.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = apiId,
                        onValueChange = { apiId = it.filter(Char::isDigit).take(10) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Telegram API ID") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = apiHash,
                        onValueChange = { apiHash = it.filter { char -> char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F' }.take(32) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Telegram API Hash") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            onSaveApiConfig(apiId, apiHash)
                            apiHash = ""
                        },
                        enabled = apiId.toIntOrNull() != null && apiId.toInt() > 0 &&
                            apiHash.length == 32 && !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Сохранить и подключить")
                    }
                }

                TelegramAuthState.WAITING_FOR_PHONE -> {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Номер телефона") },
                        placeholder = { Text("+380...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                    )
                    Button(
                        onClick = onSubmitPhone.bind(phoneNumber),
                        enabled = phoneNumber.isNotBlank() && !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Отправить код")
                    }
                }

                TelegramAuthState.WAITING_FOR_CODE -> {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Код подтверждения") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val submittedCode = code
                            code = ""
                            onSubmitCode(submittedCode)
                        },
                        enabled = code.isNotBlank() && !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Подтвердить")
                    }
                }

                TelegramAuthState.WAITING_FOR_PASSWORD -> {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Пароль двухэтапной аутентификации") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val submittedPassword = password
                            password = ""
                            onSubmitPassword(submittedPassword)
                        },
                        enabled = password.isNotBlank() && !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Подтвердить")
                    }
                }

                TelegramAuthState.ERROR, TelegramAuthState.CLOSED -> {
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text("Повторить подключение")
                    }
                }

                TelegramAuthState.READY -> {
                    Text(
                        text = "Telegram подключён. Выберите каналы для мониторинга.",
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Button(onClick = onSelectChannels, modifier = Modifier.fillMaxWidth()) {
                        Text("Выбрать каналы")
                    }
                }

                TelegramAuthState.WAITING_FOR_OTHER_DEVICE_CONFIRMATION -> {
                    Text("Подтвердите вход в Telegram на другом устройстве.")
                }

                TelegramAuthState.INITIALIZING,
                TelegramAuthState.CLOSING,
                TelegramAuthState.UNKNOWN -> {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (state.isSubmitting) {
                Spacer(modifier = Modifier.height(4.dp))
                CircularProgressIndicator()
            }

            Text(
                text = "Авторизация выполняется непосредственно через TDLib. " +
                    "Код и пароль не сохраняются приложением.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun TelegramAuthState.toDisplayText(): String = when (this) {
    TelegramAuthState.NOT_CONFIGURED -> "Не настроены параметры Telegram API"
    TelegramAuthState.INITIALIZING -> "Подготовка защищённого подключения…"
    TelegramAuthState.WAITING_FOR_PHONE -> "Введите номер телефона аккаунта Telegram"
    TelegramAuthState.WAITING_FOR_CODE -> "Введите код, отправленный Telegram"
    TelegramAuthState.WAITING_FOR_PASSWORD -> "Требуется пароль двухэтапной аутентификации"
    TelegramAuthState.WAITING_FOR_OTHER_DEVICE_CONFIRMATION ->
        "Ожидается подтверждение входа на другом устройстве"
    TelegramAuthState.READY -> "Подключение активно"
    TelegramAuthState.CLOSING -> "Завершение подключения…"
    TelegramAuthState.CLOSED -> "Подключение закрыто"
    TelegramAuthState.ERROR -> "Не удалось подключиться к Telegram"
    TelegramAuthState.UNKNOWN -> "Получено неизвестное состояние авторизации"
}

private fun ((String) -> Unit).bind(value: String): () -> Unit = { this(value) }
