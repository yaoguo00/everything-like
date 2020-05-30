package task;

import java.io.File;

/**
 * @author guoyao
 * @create 2020/2/14
 */
public interface ScanCallback {

    //对于文件夹的扫描进行回调，处理文件夹,将文件夹下一级的子文件夹，子文件夹保存到数据库
    void callback(File dir);

}
