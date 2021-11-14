package com.karthek.android.s.files2.state

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore(name = "prefs")

val SHOW_HIDDEN_KEY = booleanPreferencesKey("show_hidden")
val SORT_PREF_KEY = intPreferencesKey("sort_pref")

data class UserPrefs(val showHidden: Boolean, val sortPref: Int)

@Singleton
class Prefs @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.dataStore

    val prefsFlow = dataStore.data.map { prefs ->
        val showHidden = prefs[SHOW_HIDDEN_KEY] ?: false
        val sortPref = prefs[SORT_PREF_KEY] ?: 1
        UserPrefs(showHidden, sortPref)
    }

    suspend fun onShowHiddenChange(showHidden: Boolean) {
        dataStore.edit {
            it[SHOW_HIDDEN_KEY] = showHidden
        }
    }

    suspend fun onSortPrefChange(pref: Int) {
        dataStore.edit {
            it[SORT_PREF_KEY] = pref
        }
    }
}

