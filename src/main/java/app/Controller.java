package app;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import task.*;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    private GridPane rootPane;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<FileMeta> fileTable;

    @FXML
    private Label srcDirectory;

    private Thread task;

    public void initialize(URL location, ResourceBundle resources) {
        //界面初始化，需要初始化数据库和数据库表
        DBInit.init();
        // 添加搜索框监听器，内容改变时执行监听事件
        searchField.textProperty().addListener(new ChangeListener<String>() {

            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                freshTable();
            }
        });
    }

    public void choose(Event event) {
        // 选择文件目录
        DirectoryChooser directoryChooser=new DirectoryChooser();
        Window window = rootPane.getScene().getWindow();
        File file = directoryChooser.showDialog(window);
        if(file == null)
            return;
        // 获取选择的目录路径，并显示
        String path = file.getPath();
        // TODO
        srcDirectory.setText(path);
        //选择了目录，就需要执行目录的扫描任务：将该目录下的素有子文件和子文件夹全扫描出来
       if(task!=null){
           //中断线程
           task.interrupt();
       }

        task=new Thread(new Runnable() {
            @Override
            public void run() {
                //文件扫描回调接口，做文件夹下一级子文件和子文件保存数据库的操作
                ScanCallback callback=new FileSave();
                //传入扫描任务类
                FileScanner scanner=new FileScanner(callback);
                try {
                    System.out.println("执行文件扫描任务：");
                    scanner.scan(path);  //为了提高效率，多线程执行扫描任务（根目录）
                    //等待文件扫描任务执行完毕,waitFinsh()需要阻塞等待
                    System.out.println("等待扫描任务结束："+path);
                    scanner.waitFinish();
                    System.out.println("任务执行完毕，刷新表格");
                    //刷新表格：将扫描出来的子文件，子文件夹都展示在表格里面
                    freshTable();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    //scanner.shutdown();
                }
            }
        });
        task.start();
    }

    // 刷新表格数据
    private void freshTable(){
        ObservableList<FileMeta> metas = fileTable.getItems();
        metas.clear();
        // 如果是选择了某个目录，代表需要再根据搜索框的内容，进行数据库文件的查询
        String dir=srcDirectory.getText();
        if(dir!=null&& dir.trim().length()!=0){
            String content=searchField.getText();
            //提供数据库的查询方法
            List<FileMeta> fileMetas= FileSearch.search(dir,content);
            //TODO
            //Connection -->List/Set--->ArrayList+LinkedList/HashSet+TreeSet
            //Map-->HahsMap/TreeMap/HashTable

            metas.addAll(fileMetas);
        }
        //--->方法返回后，javafx表单做什么
        //通过反射获取fileMeta类型中的属性（app.fxal文件中定义的属性）
    }


}