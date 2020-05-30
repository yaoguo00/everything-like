package task;

import app.FileMeta;
import util.DBUtil;
import util.PinyinUtil;
import util.Util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author guoyao
 * @create 2020/2/14
 */
public class FileSave implements ScanCallback{
    @Override
    public void callback(File dir) {
        //文件夹下一级子文件和子文件夹保存数据库
        //获取本地目录下一级子文件和子文件夹
        //集合框架中使用自定义类型，判断是否某个对象在集合中存在：比对两个集合中的元素

        File[] children=dir.listFiles();
        List<FileMeta> locals=new ArrayList<>();
        if(children!=null){
            for(File child:children){
            locals.add(new FileMeta(child));
            }
        }
        //获取数据库保存的dir目录的下一级子文件和子文件夹(jdbc select)
        //TODO List<File>

        List<FileMeta> metas=query(dir);

        //数据库有，本地没有，做删除(delete)


        for(FileMeta meta:metas){
            if(!locals.contains(meta)){
                //meta的删除：
                //1.删除meta信息本身
                //2.如果meta是目录，还要让meta的所有子文件、子文件夹删除
                //TODO delete
                delete(meta);
            }
        }
        //本地有，数据库没有，做插入(insert)
        //TODO
        for(FileMeta meta:locals){
            if(!metas.contains(meta)){
                save(meta);
            }
        }
    }

    //meta的删除：
    //1.删除meta信息本身
    //2.如果meta是目录，还要让meta的所有子文件、子文件夹删除
    private void delete(FileMeta meta) {
        Connection connection=null;
        PreparedStatement ps=null;
        try{
            connection=DBUtil.getConnevtion();
            String sql="delete from file_meta where" +
                    " (name=? and path=? and is_directory=?)";
            if(meta.getDirectory()){
                sql+=" or path=?" +   //匹配数据文件夹的儿子
                        " or path like ?" ;  //匹配数据文件夹的孙后辈
            }
            ps=connection.prepareStatement(sql);
            ps.setString(1,meta.getName());
            ps.setString(2,meta.getPath());
            ps.setBoolean(3,meta.getDirectory());
          if(meta.getDirectory()){
              ps.setString(4,
                      meta.getPath()+File.separator+meta.getName());
              ps.setString(5,
                      meta.getPath()+File.separator+meta.getName()+File.separator);
          }
            System.out.printf("删除文件信息，dir=%s\n",
                    meta.getPath()+File.separator+meta.getName());
          ps.executeUpdate();
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("删除文件出错，检查delete语句",e);
        }finally {
            DBUtil.close(connection,ps);
        }

    }

    /**
     * 查询操作
     * @param dir
     * @return
     */
    private List<FileMeta> query(File dir){
        Connection connection=null;
        PreparedStatement ps=null;
        ResultSet rs=null;
        List<FileMeta> metas=new ArrayList<>();
        try{
            //1.创建数据库连接
            connection=DBUtil.getConnevtion();
            String sql="select name,path,is_directory,size,last_modified" +
                    " from file_meta where path=?";
            //2.创建jdbc操作命令statement
            ps=connection.prepareStatement(sql);
            ps.setString(1,dir.getPath());
            //3.执行sql语句
            rs=ps.executeQuery();
            //4.处理结果集ResultSet
            while(rs.next()){
                String name=rs.getString("name");
                String path=rs.getString("path");
                Boolean isDirextory=rs.getBoolean("is_directory");
                Long size=rs.getLong("size");
                Timestamp lastModified=rs.getTimestamp("last_modified");
                FileMeta meta=new FileMeta(name,path,isDirextory,
                        size,new java.util.Date(lastModified.getTime()));
                System.out.printf("查询文件信息：name=%s,path=%s,is_directory=%s," +
                        " size=%s,last_modified=%s\n",name,path,String.valueOf(isDirextory),
                        String.valueOf(size), Util.parseDate(new java.util.Date(lastModified.getTime())));
                metas.add(meta);

            }

            return metas;
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("查询文件信息出错，检查sql语句",e);
        }finally {
            DBUtil.close(connection,ps,rs);
        }
    }


    /**
     * 文件信息保存到数据库
     * @param
     */
    private void save(FileMeta meta){

        Connection connection=null;
        PreparedStatement statement=null;
        try {
            //1.获取数据库连接
            connection = DBUtil.getConnevtion();
            String sql = "insert into file_meta" +
                    "(name,path,is_directory,size,last_modified,pinyin,pinyin_first) " +
                    " values (?,?,?,?,?,?,?)";
            //2.获取sql操作命令对象statement
            statement = connection.prepareStatement(sql);
            statement.setString(1, meta.getName());
            statement.setString(2, meta.getPath());
            statement.setBoolean(3, meta.getDirectory());
            statement.setLong(4, meta.getSize());
            statement.setString(5, meta.getLastModifiedText());
            statement.setString(6, meta.getPinyin());
            statement.setString(7, meta.getPinyinFirst());

            String pinyin = null;
            String pinyin_first = null;
            //文件包含汉字，需要获取拼音和拼音首字母，并保存到数据库
            if (PinyinUtil.containsChinese(meta.getName())) {
                String[] pinyins = PinyinUtil.get(meta.getName());
                pinyin = pinyins[0];
                pinyin_first = pinyins[1];
            }
            statement.executeUpdate();
        }catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("文件保存失败，检查sql insert语句",e);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            //4.释放资源
            DBUtil.close(connection,statement);
        }
    }

    public static void main(String[] args) {
//        DBInit.init();
//        File file =new File("G:\\有用文件\\课件");
//        FileSave fileSave=new FileSave();
//        fileSave.save(file);
//        fileSave.query(file.getParentFile());
        List<FileMeta> locals=new ArrayList<>();
       locals.add(new FileMeta("新建文件夹",
               "G:\\项目\\maven-test文件",
               true,0,new Date()));
        locals.add(new FileMeta("中华人民共和国",
                "G:\\项目\\maven-test文件",
                true,0,new Date()));
        locals.add(new FileMeta("阿凡达.txt",
                "G:\\项目\\maven-test文件\\中华人民共和国",
                true,0,new Date()));
        List<FileMeta> metas=new ArrayList<>();
        metas.add(new FileMeta("新建文件夹",
                "G:\\项目\\maven-test文件",
                true,0,new Date()));
        metas.add(new FileMeta("中华人民共和国2",
                "G:\\项目\\maven-test文件",
                true,0,new Date()));
        metas.add(new FileMeta("阿凡达.txt",
                "G:\\项目\\maven-test文件\\中华人民共和国2",
                true,0,new Date()));
        Boolean contains=locals.contains(new File(""));
        //集合中是否包含某个元素，不一定传入这个对象在Java内存中是同一个对象的引用
        //满足一定条件（集合中的元素类型需要hashcode和equals
        // ---->业务需要哪些属性来判断来表示同一个）
        //list.contains()方法可以返回true
        for(FileMeta meta:locals){
            if(!metas.contains(meta)){
                System.out.println(meta);
            }
        }
    }
}
