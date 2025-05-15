package es.uniovi.amigos

import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService: FirebaseMessagingService() {
    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    override fun onNewToken(token: String) {
        Log.d("FCM", "Token generado: $token")

        // Guarda el token en SharedPreferences
        val prefs = applicationContext.getSharedPreferences("prefs", MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data["action"]?.let { action ->
            if (action == "location_updated") {
                // El servicio emite un broadcast cuando se actualiza la localización
                val intent = Intent("LOCATION_UPDATED")
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }
        }
    }

}