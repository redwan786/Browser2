package com.example.multiprofilebrowser

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfileAdapter
    private lateinit var profiles: MutableList<Profile>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerProfiles)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // নতুন Profile যোগ করার button
        findViewById<ImageButton>(R.id.btnAddProfile).setOnClickListener {
            addNewProfile()
        }
    }

    override fun onResume() {
        super.onResume()
        // Browser থেকে ফিরে আসলে list refresh করো
        profiles = ProfileManager.loadProfiles(this)
        adapter = ProfileAdapter(profiles,
            onProfileClick = { index -> openBrowser(index) },
            onRename = { index -> renameProfile(index) },
            onDelete = { index -> deleteProfile(index) }
        )
        recyclerView.adapter = adapter
    }

    // Browser Activity খোলো
    private fun openBrowser(index: Int) {
        val intent = Intent(this, BrowserActivity::class.java)
        intent.putExtra("profile_index", index)
        startActivity(intent)
    }

    // নতুন Profile যোগ করো
    private fun addNewProfile() {
        val count = profiles.size + 1
        val newProfile = Profile(
            id = ProfileManager.generateId(),
            name = "Profile $count",
            tabs = mutableListOf(
                Tab(ProfileManager.generateId(), "Google", "https://www.google.com")
            )
        ).apply { activeTabId = tabs.first().id }

        profiles.add(newProfile)
        ProfileManager.saveProfiles(this, profiles)
        adapter.notifyItemInserted(profiles.size - 1)
    }

    // Profile এর নাম পরিবর্তন করো
    private fun renameProfile(index: Int) {
        val editText = android.widget.EditText(this)
        editText.setText(profiles[index].name)
        AlertDialog.Builder(this)
            .setTitle("Profile এর নাম পরিবর্তন করো")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                profiles[index].name = editText.text.toString()
                ProfileManager.saveProfiles(this, profiles)
                adapter.notifyItemChanged(index)
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    // Profile delete করো
    private fun deleteProfile(index: Int) {
        if (profiles.size <= 1) {
            android.widget.Toast.makeText(this, "কমপক্ষে একটা profile থাকতে হবে", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Profile Delete করবে?")
            .setMessage("\"${profiles[index].name}\" এবং এর সব tabs মুছে যাবে।")
            .setPositiveButton("Delete") { _, _ ->
                profiles.removeAt(index)
                ProfileManager.saveProfiles(this, profiles)
                adapter.notifyItemRemoved(index)
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }
}

// ============================================================
// ProfileAdapter — RecyclerView এর জন্য
// ============================================================
class ProfileAdapter(
    private val profiles: MutableList<Profile>,
    private val onProfileClick: (Int) -> Unit,
    private val onRename: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileIcon: TextView = view.findViewById(R.id.profileIcon)
        val profileName: TextView = view.findViewById(R.id.profileName)
        val profileTabCount: TextView = view.findViewById(R.id.profileTabCount)
        val btnMenu: ImageButton = view.findViewById(R.id.btnProfileMenu)
        val profileItem: View = view.findViewById(R.id.profileItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = profiles[position]
        holder.profileIcon.text = profile.name.first().toString().uppercase()
        holder.profileName.text = profile.name
        val tabCount = profile.tabs.size
        holder.profileTabCount.text = "$tabCount tab${if (tabCount > 1) "s" else ""}"

        holder.profileItem.setOnClickListener { onProfileClick(position) }

        holder.btnMenu.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(view.context, view)
            popup.menu.add("নাম পরিবর্তন")
            popup.menu.add("Delete")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "নাম পরিবর্তন" -> onRename(position)
                    "Delete" -> onDelete(position)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = profiles.size
}