package app.lightalert.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.scheduleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lightalert_schedule",
)

fun Context.scheduleDataStore(): DataStore<Preferences> = scheduleDataStore
