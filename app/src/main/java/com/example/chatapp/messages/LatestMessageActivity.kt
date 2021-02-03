package com.example.chatapp.messages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.application.RetrofitInstance
import com.example.chatapp.messages.NewMessageActivity.Companion.USER_KEY
import com.example.chatapp.models.ChatMessage
import com.example.chatapp.models.NotificationData
import com.example.chatapp.models.PushNotification
import com.example.chatapp.models.User
import com.example.chatapp.registration.RegisterActivity
import com.example.chatapp.views.LatestMessageRow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception

const val topic = "/topics/myTopic"

class LatestMessageActivity: AppCompatActivity() {

    val latestMessagesMap = HashMap<String, ChatMessage>()
    val adapter = GroupAdapter<ViewHolder>()
    lateinit var noMessageImage: TextView
    lateinit var noMessageText: TextView
    lateinit var recyclerView: RecyclerView

    //test
    lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_latest_message)
        setUpView()
        listenForLatestMessages()
        fetchCurrentUser()
        verifyUserIsLoggedIn()
        super.onCreate(savedInstanceState)
    }

    private fun setUpView() {
        recyclerView = findViewById(R.id.recyclerview_latest_messages)
        noMessageImage = findViewById(R.id.no_messages_icon_latest_activity)
        noMessageText = findViewById(R.id.no_message_text_latest_activity)

        button = findViewById(R.id.testing_will_delete_later)
        FirebaseMessaging.getInstance().subscribeToTopic(topic)

        button.setOnClickListener {
            val title = "test"
            val message = "working"
            val topic = topic
            if (title.isNotEmpty() && message.isNotEmpty()) {
                PushNotification(
                    NotificationData(title, message),
                    topic
                ).also {
                    sendNotification(it)
                }
            }
        }

        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        adapter.setOnItemClickListener { item, view ->
            val intent = Intent(this, ChatLogActivity::class.java)
            val row = item as LatestMessageRow
            intent.putExtra(USER_KEY, row.chatPartnerUser)
            startActivity(intent)
        }
    }



    private fun listenForLatestMessages() {
        val fromId = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId")
        ref.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                noMessageImage.visibility = View.GONE
                noMessageText.visibility = View.GONE
                refreshRecyclerViewMessages()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshRecyclerViewMessages()
            }


            override fun onCancelled(error: DatabaseError) {
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }
        })
    }


    private fun refreshRecyclerViewMessages() {
        adapter.clear()
        latestMessagesMap.values.forEach {
            adapter.add(LatestMessageRow(it))
        }
    }

    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun verifyUserIsLoggedIn() {
        val uid = FirebaseAuth.getInstance().uid
        if (uid == null) {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_new_message -> {
                val intent = Intent(this, NewMessageActivity::class.java)
                startActivity(intent)
            }

            R.id.menu_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // notification

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                Log.d("LatestMessageActivity", Gson().toJson(response))
            } else {
                Log.d("LatestMessageActivity", response.errorBody().toString())
            }
        } catch (e: Exception) {
            Log.e("LatestMessageActivity", e.toString())
        }
    }



    companion object {
        var currentUser: User? = null
    }

}