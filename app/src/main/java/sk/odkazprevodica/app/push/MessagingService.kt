package sk.odkazprevodica.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import sk.odkazprevodica.app.data.Repository

/**
 * Prijíma push správy zo servera. Server posiela iba dáta (bez hotovej
 * notifikácie), aby aplikácia mohla zobraziť vlastnú notifikáciu s veľkým písmom.
 */
class MessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Repository.init(applicationContext)
        Repository.onNewPushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val d = message.data
        val title = d["title"] ?: return
        val body = d["body"] ?: return
        Notifications.show(
            context = applicationContext,
            id = (d["messageId"] ?: title).hashCode(),
            title = title,
            body = body,
            plate = d["plate"],
            urgent = d["urgent"] == "1",
        )
    }
}
