package app.lightalert.data.local.db

import android.content.Context
import androidx.room.Room

object LightAlertDatabaseProvider {
    @Volatile
    private var instance: LightAlertDatabase? = null

    fun get(context: Context): LightAlertDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LightAlertDatabase::class.java,
                DATABASE_NAME,
            ).build().also { database ->
                instance = database
            }
        }

    private const val DATABASE_NAME = "lightalert_history.db"
}
