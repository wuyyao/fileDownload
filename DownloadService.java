package hunnu.edu.cn.dowloaddemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.File;

public class DownloadService extends Service {
    //下载的异步操作类
    private DownloadTask downloadTask;
    private TaskInfo info; //下载信息的JavaBean

    private static final String TAG = "DownloadService";


    private DownloadListener listener = new DownloadListener() {
        //对下载过程中的各种状态进行监听和回调
        //更新下载状态
        @Override
        public void onProgress(int progress) {
            Log.d(TAG, "onProgress: -------" + progress);
            getNotificationManager().notify(1, getNotification("Downloading...", progress));
        }

        //下载成功
        @Override
        public void onSuccess() {
            Log.d(TAG, "onSuccess: -------------");
            downloadTask = null;
            //下载成功时将前台服务通知关闭，并创建一个下载成功的通知
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("Download Success", -1));
            Toast.makeText(DownloadService.this, "Download Success", Toast.LENGTH_SHORT).show();
        }

        //下载失败
        @Override
        public void onFailed() {
            Log.d(TAG, "onFailed: -------------");
            downloadTask = null;
            // 下载失败时将前台服务通知关闭，并创建一个下载失败的通知
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("Download Failed", -1));
            Toast.makeText(DownloadService.this, "Download Failed", Toast.LENGTH_SHORT).show();
        }

        //下载暂停
        @Override
        public void onPaused() {
            Log.d(TAG, "onPaused: -------------");
            downloadTask = null;
            Toast.makeText(DownloadService.this, "Download Paused", Toast.LENGTH_SHORT).show();
        }

        //取消下载
        @Override
        public void onCanceled() {
            Log.d(TAG, "onCanceled: -------------");
            downloadTask = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this, "Download Canceled", Toast.LENGTH_SHORT).show();
        }
    };

    private DownloadBinder mBinder = new DownloadBinder();


    // 创建 DownloadBinder 实例
    class DownloadBinder extends Binder {
        DownloadService getService()
        {
            return DownloadService.this;
        }
    }
    //返回DownloadBinder的实例
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        pauseDownload();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    public void startDownload(String url) {
        Log.d(TAG, "startDownload--------: 开始下载");
        if (downloadTask == null) {
            this.info = new TaskInfo(url);
            //创建TaskInfo实例，并作创建downloadTask实例的参数
            //然后启动异步任务，开始下载
            downloadTask = new DownloadTask(listener,info);
            downloadTask.execute();
            startForeground(1, getNotification("DownLoading...", 0));
            Toast.makeText(DownloadService.this, "Downloading...", Toast.LENGTH_SHORT).show();

        }
    }

    public void pauseDownload() {
        if (downloadTask != null) {
            downloadTask.pauseDownload();
        }
    }

    public void cancelDownload() {
        if (downloadTask != null) {
            downloadTask.cancelDownload();
        }
        String downloadUrl = info.getUrl();
        if (downloadUrl != null) {
            // 取消下载时需将文件删除，并将通知关闭
            //String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            //String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            String fileName = info.getName();
            String directory = info.getPath();
            File file = new File(directory + fileName);
            if (file.exists()) {
                file.delete();
            }
            getNotificationManager().cancel(1);
            stopForeground(true);
            Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();
        }
    }

    //当前下载进度
    int getCurrentDownloadProgress() {
        //计算下载进度
        int currentProgress = (int) ((float) info.getCompletedLen() / (float) info.getContentLen() * 100);
        return currentProgress;
    }
    //当前的下载状态
    int getCurrentStatus()
    {
        return info.getStatus();
    }

    /*获取通知服务管理器*/
    private NotificationManager getNotificationManager()
    {
        return (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    //配置通知并获取通知实例后返回该通知对象
    private Notification getNotification(String title, int progress)
    {
        /*Intent[] intents = new Intent[]{(new Intent(this, MainActivity.class))};
        PendingIntent pi = PendingIntent.getActivities(this, 0, intents, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if (progress > 0) {
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        return builder.build();*/

        // 设置启动的程序，如果存在则找出，否则新的启动
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(this, MainActivity.class));//用ComponentName得到class对象
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);// 关键的一步，设置启动模式，两种情况
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);//将经过设置了的Intent绑定给PendingIntent
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(contentIntent);
        builder.setContentTitle(title);
        if (progress > 0) {
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }
}
