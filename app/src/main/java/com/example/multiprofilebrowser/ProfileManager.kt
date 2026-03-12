package com.example.multiprofilebrowser

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ============================================================
// Tab — একটা browser tab এর data
// ============================================================
data class Tab(
    val id: String,           // unique id
    var title: String,        // tab এর title
    var url: String           // tab এর current URL
)

// ============================================================
// Profile — একটা browser profile এর data
// ============================================================
data class Profile(
    val id: String,           // unique id
    var name: String,         // profile এর নাম
    var tabs: MutableList<Tab> = mutableListOf(),  // এই profile এর সব tabs
    var activeTabId: String = ""  // কোন tab এখন active
)

// ============================================================
// ProfileManager — profiles save ও load করে
// SharedPreferences ব্যবহার করে phone এ data save করে
// ============================================================
object ProfileManager {

    private const val PREF_NAME = "profiles_data"
    private const val KEY_PROFILES = "profiles"
    private val gson = Gson()

    // সব profiles load করো phone থেকে
    fun loadProfiles(context: Context): MutableList<Profile> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PROFILES, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Profile>>() {}.type
            gson.fromJson(json, type)
        } else {
            // প্রথমবার চালালে default 2টা profile বানাও
            mutableListOf(
                Profile(
                    id = "profile_1",
                    name = "Profile 1",
                    tabs = mutableListOf(Tab("tab_1", "Google", "https://www.google.com")),
                    activeTabId = "tab_1"
                ),
                Profile(
                    id = "profile_2",
                    name = "Profile 2",
                    tabs = mutableListOf(Tab("tab_2", "Google", "https://www.google.com")),
                    activeTabId = "tab_2"
                )
            )
        }
    }

    // সব profiles save করো phone এ
    fun saveProfiles(context: Context, profiles: MutableList<Profile>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILES, gson.toJson(profiles)).apply()
    }

    // নতুন unique ID বানাও
    fun generateId(): String = "id_${System.currentTimeMillis()}"
}