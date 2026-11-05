package app.lightalert.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SourceMessageEntity::class,
        ScheduleRevisionEntity::class,
        EffectiveQueueEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LightAlertDatabase : RoomDatabase() {
    abstract fun lightAlertDao(): LightAlertDao
}
