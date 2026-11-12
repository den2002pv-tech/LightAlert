package app.lightalert.presentation.telegram

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lightalert.data.telegram.TelegramAuthState
import app.lightalert.data.telegram.TelegramChatSummary

@Composable
fun TelegramChannelScreen(
    state: TelegramChannelUiState,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onToggle: (Long) -> Unit,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onBack) { Text("Назад") }
                Text(
                    text = "Выбрано: ${state.selectedChatIds.size}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = "Каналы и группы",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Отметьте Telegram-каналы и группы, из которых приложение будет получать расписания отключений.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск каналов") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onRefresh,
                    enabled = !state.isLoading && state.authState == TelegramAuthState.READY,
                ) { Text("Обновить список") }
                if (state.isLoading) CircularProgressIndicator()
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (state.authState != TelegramAuthState.READY) {
                Text(
                    "Нет активного подключения Telegram. Вернитесь и завершите авторизацию.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (!state.isLoading && state.chats.isEmpty() && state.errorMessage == null) {
                Text("Список чатов пока пуст. Нажмите «Обновить список».")
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.filteredChats, key = TelegramChatSummary::id) { chat ->
                    ChannelRow(
                        chat = chat,
                        selected = chat.id in state.selectedChatIds,
                        onToggle = { onToggle(chat.id) },
                    )
                }
            }
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && state.authState == TelegramAuthState.READY,
            ) {
                if (state.isSaving) CircularProgressIndicator()
                else Text("Сохранить выбор (${state.selectedChatIds.size})")
            }
        }
    }
}

@Composable
private fun ChannelRow(
    chat: TelegramChatSummary,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(chat.title, fontWeight = FontWeight.SemiBold)
                Text(
                    chat.kind,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                val details = buildString {
                    if (chat.unreadCount > 0) append("Непрочитано: ${chat.unreadCount}")
                    chat.lastMessagePreview?.takeIf(String::isNotBlank)?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it.take(90))
                    }
                }
                if (details.isNotBlank()) {
                    Text(
                        details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
