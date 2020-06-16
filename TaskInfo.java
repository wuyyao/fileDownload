package hunnu.edu.cn.dowloaddemo;

import android.os.Environment;
import android.util.Log;

import java.io.File;

public class TaskInfo {
    private int status=-1; // 下载成功、失败、暂停、取消、正在下载的状态，0,1,2,3,4
    private String name;//文件名
    private String path;//文件路径
    private String url;//链接
    private long contentLen;//文件总长度
    /**
     * 迄今为止java虚拟机都是以32位作为原子操作，而long与double为64位，当某线程
     * 将long/double类型变量读到寄存器时需要两次32位的操作，如果在第一次32位操作
     * 时变量值改变，其结果会发生错误，简而言之，long/double是非线程安全的，volatile
     * 关键字修饰的long/double的get/set方法具有原子性。
     */
    private volatile long completedLen;//已完成长度

    public TaskInfo(String name, String path, String url) {
        this.name = name;
        this.path = path;
        this.url = url;
    }
    public TaskInfo(String url) {
        this.url = url;
        this.name = url.substring(url.lastIndexOf("/"));
        //this.path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        path = Environment.getExternalStorageDirectory()+ "/Download/";

        File file = new File(path + name);
        this.completedLen = file.length();
        Log.e("Service:completedLen",this.completedLen+"");
        //this.completedLen = 0;
        this.contentLen =0 ;
    }

    public int getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getUrl() {
        return url;
    }

    public long getContentLen() {
        return contentLen;
    }

    public long getCompletedLen() {
        return completedLen;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setContentLen(long contentLen) {
        this.contentLen = contentLen;
    }

    public void setCompletedLen(long completedLen) {
        this.completedLen = completedLen;
    }
}
