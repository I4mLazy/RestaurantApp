package com.example.restaurantapp.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.restaurantapp.models.Reservation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.example.restaurantapp.R;

public class MessagingService extends FirebaseMessagingService
{
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage)
    {
        // Handle FCM messages here
        if (remoteMessage.getNotification() != null)
        {
            /*sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody()
            );*/
        }
    }

    @Override
    public void onNewToken(@NonNull String token)
    {
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token)
    {
        // Save the token to Firestore under the user's document
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null)
        {
            String userId = auth.getCurrentUser().getUid();
            db.collection("Users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d("FCM", "Token saved successfully"))
                    .addOnFailureListener(e -> Log.e("FCM", "Failed to save token", e));
        }
    }

    private void sendCancellationNotification(Reservation reservation)
    {
        // Build the notification title and message
        String title = "Reservation Cancelled";
        String message = "Dear " + reservation.getName() + ", your reservation for " +
                reservation.getGuests() + " guests on " +
                reservation.getDate() + " at " + reservation.getTime() + " has been cancelled by the restaurant.";

        // Send the notification to the user
        //sendNotification(title, message);
    }

    /*private void sendNotification(String title, String messageBody)
    {
        Intent intent = new Intent(this, );
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        String channelId = "fcm_default_channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.baseline_notifications_24)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }*/
}
