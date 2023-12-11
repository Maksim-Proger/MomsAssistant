package PozMaxPav.com.model.helperClasses.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import PozMaxPav.com.R;

public class NotificationService extends Service {

    public static final String ACTION_START = "Action_Start";
    private static final String CHANNEL_ID = "Channel_id";
    private static final int NOTIFICATION_ID = 1;
    private Handler handler;
    private Boolean isRunning = false;
    private long startTime = 0;
    private long timePassed = 0;
    private Handler notificationHandler = new Handler();
    private static final int NOTIFICATION_UPDATE_INTERVAL = 1000;
    private NotificationCompat.Builder builder;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        createNotificationChannel();

        // Создаем builder
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Продолжительность сна")
                .setContentText("Прошло " + formatTime(timePassed))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(null)
                .setAutoCancel(true);

        // указывает системе отобразить звуковое оповещение только один раз, и игнорировать
        // последующие обновления уведомления.
        builder.setOnlyAlertOnce(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(ACTION_START)) {
                    if (!isRunning) {
                        startTime = SystemClock.elapsedRealtime() - timePassed;
                        isRunning = true;
                        handler = new Handler();
                        startTimer();
                    }
                }
            }
        }
        showNotification();

        // startForeground нужен, чтобы избежать случайного закрытия службы системой
        startForeground(NOTIFICATION_ID, builder.build()); // после обновления до android 14
                                                           // пришлось явно указать тип переднего
                                                           // плана для службы в манифесте
                                                           // и добавить разрешение

        // Возвращаем START_STICKY, чтобы сервис автоматически
        // перезапускался, если его убьет система.
        return START_STICKY;
    }

    private void createNotificationChannel() {
        CharSequence name = "My Channel";
        String description = "Описание канала уведомлений";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void showNotification() {
        if (builder != null) {
            // Используем ранее созданный builder
            builder.setContentText("Прошло " + formatTime(timePassed));

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.notify(NOTIFICATION_ID, builder.build());

            updateNotification();
        }
    }

    private void updateNotification() {
        notificationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showNotification();
            }
        }, NOTIFICATION_UPDATE_INTERVAL);
    }

    private void startTimer() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    timePassed = SystemClock.elapsedRealtime() - startTime;
                    Intent intent = new Intent("UPDATE_TIME");
                    intent.putExtra("timePassed", timePassed);
                    sendBroadcast(intent);
                    LocalBroadcastManager.getInstance(NotificationService.this).sendBroadcast(intent);
                    handler.postDelayed(this, 1000); // Обновляем каждую секунду
                }
            }
        });
    }


    private String formatTime(long time) {
        long seconds = (time / 1000) % 60;
        long minutes = (time / (1000 * 60)) % 60;
        long hours = (time / (1000 * 60 * 60));

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (notificationHandler != null) {
            notificationHandler.removeCallbacksAndMessages(null);
        }
        // Удалите уведомление, если оно было создано
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
