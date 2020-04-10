package com.example.socialapplication.Notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.example.socialapplication.Activity.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessaging : FirebaseMessagingService() {

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)

        val sp = getSharedPreferences("SP_USER", Context.MODE_PRIVATE)
        val saveCurrentUser = sp.getString("Current_USERID", "None")

        val sent = p0.data.get("sent")
        val user = p0.data.get("user")
        val fUser = FirebaseAuth.getInstance().currentUser
        if (fUser != null && sent.equals(fUser!!.uid)){

            if (!saveCurrentUser.equals(user)){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

                    sendOAndAboveNotification(p0)

                }else{

                    sendNormalNotification(p0)

                }

            }

        }

    }

    private fun sendNormalNotification(p0: RemoteMessage) {

        val user = p0.data.get("user")
        val icon = p0.data.get("icon")
        val title = p0.data.get("title")
        val body = p0.data.get("body")

        val notification = p0.notification
        val i = Integer.parseInt(user!!.replace("[\\D]".toRegex(), ""))
        val intent = Intent(this, ChatActivity::class.java)
        val bundle = Bundle()
        bundle.putString("hisUid", user)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT)

        val defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this)
            .setSmallIcon(icon!!.toInt())
            .setContentText(body)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setSound(defSoundUri)
            .setContentIntent(pIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var j = 0
        if (i>0){
            j=1
        }
        notificationManager.notify(j, builder.build())

    }

    private fun sendOAndAboveNotification(p0: RemoteMessage) {

        val user = p0.data.get("user")
        val icon = p0.data.get("icon")
        val title = p0.data.get("title")
        val body = p0.data.get("body")

        val notification = p0.notification
        val i = Integer.parseInt(user!!.replace("[\\D]".toRegex(), ""))
        val intent = Intent(this, ChatActivity::class.java)
        val bundle = Bundle()
        bundle.putString("hisUid", user)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT)

        val defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification1 = OreoAndAboveNotification(this)
        val builder = notification1.getNotifications(title!!, body!!, pIntent, defSoundUri, icon!!)

        var j = 0
        if (i>0){
            j=1
        }
        notification1.getManager().notify(j, builder.build())

    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)

        val user : FirebaseUser? = FirebaseAuth.getInstance().currentUser
        if (user != null){
            updateToken(p0)
        }
    }

    private fun updateToken(p0: String) {

        val user = FirebaseAuth.getInstance().currentUser
        val ref : DatabaseReference? = FirebaseDatabase.getInstance().getReference("Tokens")
        val token = Token(p0)
        ref?.child(user!!.uid)?.setValue(token)
    }

}