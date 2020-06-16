package hunnu.edu.cn.dowloaddemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private DownloadService downloadService;

    //用于更新进度的Handler
    private Handler mHandler = new Handler();
    private ProgressBar progressBar;
    private TextView textStatus;

    private ServiceConnection connection = new ServiceConnection() {

        /*活动与服务解绑后*/
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        /*活动与服务绑定成功后*/
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadService = ((DownloadService.DownloadBinder) service).getService();
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button startDownload = (Button) findViewById(R.id.start_download);
        Button pauseDownload = (Button) findViewById(R.id.pause_download);
        Button cancelDownload = (Button) findViewById(R.id.cancel_download);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        textStatus = (TextView)findViewById(R.id.textStatus);
        progressBar.setMax(100);//设置进度条的最大值

        startDownload.setOnClickListener(this);
        pauseDownload.setOnClickListener(this);
        cancelDownload.setOnClickListener(this);

        Intent intent = new Intent(this, DownloadService.class);
        startService(intent); // 启动服务
        bindService(intent, connection, BIND_AUTO_CREATE); // 绑定服务
        Log.e("MainActivity","onCreate");
        //获取写的权限
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{ Manifest.permission. WRITE_EXTERNAL_STORAGE }, 1);
        }
    }

    @Override
    public void onClick(View v) {
        if (downloadService == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.start_download:
                String url = "https://www.python.org/downloads/release/python-383/python-3.8.3-amd64.exe";
                //String url = "https://www.jetbrains.com/pycharm/download/download-thanks.html?platform=windows/pycharm-professional-2020.1.1.exe";
                //String url = "https://image.so.com/z?a=viewPage&ch=food&src=banner_food&gid=&ancestor=list&clw=326#grpid=c66d9f449ba7c349e981b0c3036a0a4b&id=e50eb41d2394cedf39675203c33e1c4e&dataindex=3";
                downloadService.startDownload(url);
                mHandler.postDelayed(mRunnable,200);//开始Handler循环
                break;
            case R.id.pause_download:
                downloadService.pauseDownload();
                int i = downloadService.getCurrentDownloadProgress();
                textStatus.setText("Download task is paused!"+i+"%");
                progressBar.setProgress(i);
                mHandler.removeCallbacks(mRunnable);
                break;
            case R.id.cancel_download:
                downloadService.cancelDownload();
                progressBar.setProgress(0);
                textStatus.setText("Download task is canceled!");
                mHandler.removeCallbacks(mRunnable);
                break;
            default:
                break;
        }
    }

    /*申请权限的返回结果*/
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    /*结束活动时关闭服务*/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在Activity销毁时移除，并置空，防止内存泄露
        if(mHandler != null){
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        Log.e("MainActivity","onDestroy");
        unbindService(connection);
    }
    //返回键的点击事件
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }

    //Runnable消息用于更新进度条显示，单击"开始下载"按钮时首次将Runnable消息压入消息队列
    private  Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            //获取下载进度
            int currentProgress = downloadService.getCurrentDownloadProgress();
            //获取下载状态信息
            int currentStatus = downloadService.getCurrentStatus();
            //根据下载的状态和进度更新进度条显示和textStatus文字显示
            //能进入run方法的下载状态只会有TYPE_SUCCESS(0)，TYPE_FAILED(1)和TYPE_DOWNLOADING(4)
            //TYPE_SUCCESS(0)，TYPE_FAILED(1)时不再定时发送Runnable对象到消息队列
            //只有TYPE_DOWNLOADING(4)时需要定时将Runnable对象压入消息队列
            if (currentStatus == 0){
                mHandler.removeCallbacks(mRunnable);
                textStatus.setText("Download Success");
                progressBar.setProgress(100);
            } else if(currentStatus == 1){
                mHandler.removeCallbacks(mRunnable);
                textStatus.setText("Download Failed");
                progressBar.setProgress(0);
            } else if (currentStatus == -1){
                textStatus.setText("Waiting to begin ......");
                mHandler.postDelayed(mRunnable,200);
            } else if (currentStatus == 4){
                textStatus.setText("Downloading..."+currentProgress+"%");
                progressBar.setProgress(currentProgress);
                mHandler.postDelayed(mRunnable,200);
                if(currentProgress==100){
                    textStatus.setText("Download Success");
                }
            }
        }
    };
}
