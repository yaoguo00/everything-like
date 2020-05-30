package task;

import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author guoyao
 * @create 2020/2/13
 */
public class FileScanner {

    //1.核心线程数：始终运行的线程数量（正式工）
    //2.最大线程数：有新任务，并且当前运行线程数小于最大线程数，
    // 会创建新的线程数来处理任务（正式工+临时工）
    //3-4.超过3这个数量，4这个时间单位，
    // 2-1（最大线程数-核心线程数）这些线程（临时工）就会关闭
    //5.工作的阻塞队列
    //6.如果超出工作队列的长度，任务要处理的方式(4种策略需要大家知道)

//    private ThreadPoolExecutor pool=new ThreadPoolExecutor(
//            3,3,0, TimeUnit.MICROSECONDS,
//            new LinkedBlockingQueue<>(),new ThreadPoolExecutor.CallerRunsPolicy()
//    );
    //之前在线程讲解的方法是一种快捷连接方式newFilxedThreadPool，java包装好的
    private ExecutorService pool=Executors.newFixedThreadPool(4);

    //计数器，不传入数值，表示初始值为0
    private volatile AtomicInteger count=new AtomicInteger();
    //线程等待的锁对象
    private Object lock=new Object();  //第一种：synchronized（lock）进行wait等待

    private CountDownLatch latch=new CountDownLatch(1);//第二种实现，await进行等待
    private Semaphore semaphore=new Semaphore(0); //第三种实现：release进行等待

    private ScanCallback callback;

    public FileScanner(ScanCallback callback) {
        this.callback=callback;
    }

    /**
     * 扫描文件目录
     * 最开始不知道有多少子文件夹，不知道应该启动多少个线程
     * @param path
     */
    public void scan(String path) {
        count.incrementAndGet();  //启动根目录扫描任务，计数器++i
        doScan(new File(path));

    }

    /**
     *
     * @param dir 待处理的文件
     */
    private void doScan(File dir){

        pool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.callback(dir);    //文件保存操作
                    File[] children = dir.listFiles();  //下一级文件和文件夹
                    if (children != null) {
                        for (File child : children) {
                            if (child.isDirectory()) { //如果是文件夹，递归处理
                               // System.out.println("文件夹：" +child.getPath());
                                count.incrementAndGet();  //启动根目录扫描任务，计数器++i
                                System.out.println("当前任务数："+count.get());
                                pool.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        doScan(child);
                                    }
                                });

                            }
                            //else { //如果是文件
                            // TODO
                            //   System.out.println("文件：" + child.getPath());
                          //  }
                        }
                    }
                }finally {  //保证线程的计数，不管是否出现异常，都能进行-1操作
                    int r=count.decrementAndGet(); //减操作
                    if(r==0){
                        //第一种实现
//                        synchronized (lock){
//                            lock.notify();
//                        }
                        //第二种实现
                        //latch.countDown();
                        //第三种实现
                        semaphore.release();
                    }
                }
            }
        });
    }

    /**
     * 等待扫描任务结束（scan方法）
     * 多线程的任务等待：thread.start()
     * 1.join()：需要使用到线程Thread类的引用对象
     * 2.wait()线程间的等待
     * 加锁等待
     *
     */
    public void waitFinish() throws InterruptedException {
        //第一种实现
//
//       synchronized (lock){
//           lock.wait();
//       }

        //第二种
        //latch.await();

        //第三种
        try{
            semaphore.acquire();
        }finally {
            //阻塞等待直到任务完成，完成后需要关闭线程池
            System.out.println("关闭线程池…");
            pool.shutdownNow();
        }




    }

    /**
     * 线程关闭
     */
    public void shutdown(){

        //两个都可以关闭，一般选择第一种
        //内部实现原理：通过内部Thread.interrupt()来中断
        System.out.println("关闭线程池…");
        //shutdown:新传入任务不再接受，但是目前所有的任务(所有线程中执行的任务
        // +工作队列中的任务)还要执行完毕
//        pool.shutdown();
        //内部实现原理：通过内部Thread.stop()来停止线程，不安全
        //shutdownNow:新传入任务不再接受，目前的任务（所有线程中执行的任务）判断
        // 是否，能够停止，如果能够停止，就结束任务；如果不能，就执行任务
        pool.shutdownNow();
    }

}
