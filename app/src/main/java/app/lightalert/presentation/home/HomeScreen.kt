package app.lightalert.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    state: HomeUiState,
    onConnectTelegram: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(15.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BoltMark(
                        modifier = Modifier.size(25.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Column {
                    Text(
                        text = "LightAlert",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Мониторинг отключений",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            Text(
                text = "Расписание отключений",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Информация о плановых и текущих отключениях будет собрана в одном месте.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = onConnectTelegram,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Подключить Telegram")
            }

            Spacer(modifier = Modifier.height(28.dp))

            when {
                state.todayQueues.isNotEmpty() -> TodayQueuesCard(state.todayQueues)
                state.isScheduleConfigured -> ConfiguredScheduleCard()
                else -> EmptyScheduleCard()
            }

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = CircleShape,
                            ),
                    )
                    Text(
                        text = "Начальная версия приложения. Источники данных пока не подключены.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "LightAlert",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Версия ${state.appVersion}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyScheduleCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BoltMark(
                    modifier = Modifier.size(38.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Пока нет расписания",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Когда расписание отключений будет добавлено, здесь появятся периоды без электричества и время восстановления.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(50.dp),
            ) {
                Text(
                    text = "ОЖИДАЕТ НАСТРОЙКИ",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ConfiguredScheduleCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Text(
            text = "Расписание настроено.",
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun BoltMark(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color,
) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.56f, size.height * 0.04f)
            lineTo(size.width * 0.20f, size.height * 0.55f)
            lineTo(size.width * 0.45f, size.height * 0.55f)
            lineTo(size.width * 0.37f, size.height * 0.96f)
            lineTo(size.width * 0.82f, size.height * 0.39f)
            lineTo(size.width * 0.56f, size.height * 0.39f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
private fun TodayQueuesCard(queues: List<TodayQueueUi>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Отключения сегодня",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            queues.forEach { queue ->
                Text(
                    text = "Черга ${queue.number}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                queue.intervals.forEach { interval ->
                    Text(
                        text = "  ${formatMinutes(interval.startMinute)} – ${formatMinutes(interval.endMinute)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun formatMinutes(minuteOfDay: Int): String {
    val hours = minuteOfDay / 60
    val minutes = minuteOfDay % 60
    return "%02d:%02d".format(hours, minutes)
}
