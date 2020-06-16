package hunnu.edu.cn.dowloaddemo;


import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class DownloadTask extends AsyncTask<Void,Integer,Integer> {

    //下载成功
    public static final int TYPE_SUCCESS = 0;
    //下载失败
    public static final int TYPE_FAILED = 1;
    //暂停下载
    public static final int TYPE_PAUSED = 2;
    //取消下载
    public static final int TYPE_CANCELED = 3;
    //正在下载
    public static final int TYPE_DOWNLOADING = 4;
    //下载状态监听回调
    private DownloadListener listener;
    //存放下载地址的信息
    private TaskInfo taskInfo;
    //是否取消
    private boolean isCanceled = false;
    //是否暂停
    private boolean isPaused = false;
    //当前进度
    private int lastProgress;

    //带监听和下载信息的构造函数
    public DownloadTask(DownloadListener listener,TaskInfo info) {
        this.listener = listener;
        this.taskInfo = info;
    }



    //在后台执行具体的下载逻辑，是在子线程里面，可以执行耗时操作
    protected Integer doInBackground(Void... params) {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;
        try {
            long downloadedLength = 0; // 记录已下载的文件长度
            //获取下载的URL地址
            String downloadUrl = taskInfo.getUrl();
            //String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            //String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            //获取下载地址中的下载文件名
            String fileName = taskInfo.getName();
            String directory = taskInfo.getPath();
            //得到要保存的文件
            file = new File(directory + fileName);
            //文件存在则获取文件长度
            if (file.exists()) {
                downloadedLength = file.length();
            }
            //获取待下载文件的字节长度
            long contentLength = getContentLen(downloadUrl);
            taskInfo.setContentLen(contentLength);
            if (contentLength == 0) {
                return TYPE_FAILED;
            } else if (contentLength == downloadedLength) {
                // 已下载字节和文件总字节相等，说明已经下载完成了
                return TYPE_SUCCESS;
            }
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                // 断点下载，指定从哪个字节开始下载
                 .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                 .url(downloadUrl)
                 .build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                //读服务器响应的数据
                is = response.body().byteStream();
                //获取随机读取文件类，可以随机读取一个文件中指定位置的数据
                savedFile = new RandomAccessFile(file, "rw");
                //跳过已下载的字节
                savedFile.seek(downloadedLength);
                //指定每次读取文件缓存区的大小为1KB
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                //每次读取的字节长度
                while ((len = is.read(b)) != -1) {
                    if (isCanceled) {
                        return TYPE_CANCELED;
                    } else if (isPaused) {
                        return TYPE_PAUSED;
                    } else {
                        //读取的全部字节的长度
                        total += len;
                        //写入每次读取的字节长度
                        savedFile.write(b, 0, len);
                        taskInfo.setCompletedLen(total + downloadedLength);
                        // 计算已下载的百分比
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        if(progress>1)
                        {
                            Log.e("DownloadTask:",progress+"");
                        }
                        //更新进度条
                        publishProgress(progress);
                    }
                }
                //关闭连接，返回成功
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭输入流
                if (is != null) {
                    is.close();
                }
                //关闭文件
                if (savedFile != null) {
                    savedFile.close();
                }
                //如果取消了，就删除文件
                if (isCanceled && file != null) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;

    }


    /*在界面上更新当前的下载进度*/
    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress) {
            taskInfo.setStatus(TYPE_DOWNLOADING);
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }


    /*通知最后的下载结果*/
    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
            default:
                break;
        }
    }

    /*暂停下载*/
    public void pauseDownload() {
        isPaused = true;
    }

    /*取消下载*/
    public void cancelDownload() {
        isCanceled = true;
    }


    /*获取下载文件的长度*/
    private long getContentLen(String downloadUrl) throws IOException {
        //获取OkHttpClient
        OkHttpClient client = new OkHttpClient();
        //创建请求
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        //获取响应
        Response response = client.newCall(request).execute();
        //如果响应成功
        if (response != null && response.isSuccessful()) {
            //获取文件长度，清除响应
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }
}
