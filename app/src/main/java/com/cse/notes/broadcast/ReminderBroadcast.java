package com.cse.notes.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.cse.notes.R;

public class ReminderBroadcast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "notifyReminder")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(ContextCompat.getColor(context, R.color.colorSecondary))
                .setContentTitle(intent.getStringExtra("titleExtra"))
                .setContentText(intent.getStringExtra("messageExtra"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);

        notificationManagerCompat.notify(200, builder.build());

    }
}
