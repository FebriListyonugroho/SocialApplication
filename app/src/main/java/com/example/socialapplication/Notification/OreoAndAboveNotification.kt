package com.example.socialapplication.Notification

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build



class OreoAndAboveNotification(base : Context) : ContextWrapper(base) {

    private val ID : String = "some_id"
    private val NAME : String = "FirebaseAPP"

    private var notificationManager : NotificationManager? = null

    init {

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){

            createChannel()

        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel() {

        val notificationChannel = NotificationChannel(ID, NAME, NotificationManager.IMPORTANCE_DEFAULT)
        notificationChannel.enableLights(true)
        notificationChannel.enableVibration(true)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getManager().createNotificationChannel(notificationChannel)

    }

    fun getManager():NotificationManager {

        if (notificationManager == null){

            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        }

        return  notificationManager!!

    }

    @TargetApi(Build.VERSION_CODES.O)
    fun getNotifications(title:String, body : String, pIntent : PendingIntent, soundUri : Uri, icon : String):Notification.Builder {

        return Notification.Builder(applicationContext, ID)
            .setContentIntent(pIntent)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSmallIcon(icon.toInt())

    }

}