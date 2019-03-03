package ren.kura.utils;

import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.StreamOpenOfficeDocumentConverter;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author: LiuHao
 * @Date: 2019/1/25
 * @Time: 13:49
 * @Description: LiuHao 2019/1/25 13:49
 * 将文件通过openOffice 转换为pdf的工具（windows环境下）
 * 之前由于office版权问题，所以使用了openOffice；2.2.2的包需要单独引入，maven的库只有2.2.1的包；路径最好使用// 双斜杠，单个/会被转译
 *
 *     <dependency>
 *         <groupId>com.artofsolving</groupId>
 *         <artifactId>jodconverter</artifactId>
 *         <version>2.2.1</version>
 *     </dependency>
 *     <dependency>
 *       <groupId>org.openoffice</groupId>
 *       <artifactId>jurt</artifactId>
 *       <version>3.0.1</version>
 *     </dependency>
 *     <dependency>
 *       <groupId>org.openoffice</groupId>
 *       <artifactId>ridl</artifactId>
 *       <version>3.0.1</version>
 *     </dependency>
 *     <dependency>
 *       <groupId>org.openoffice</groupId>
 *       <artifactId>juh</artifactId>
 *       <version>3.0.1</version>
 *     </dependency>
 *     <dependency>
 *       <groupId>org.openoffice</groupId>
 *       <artifactId>unoil</artifactId>
 *       <version>3.0.1</version>
 *     </dependency>
 *
 * 1.对需要文件和目标文件进行判断
 * 2.对目标文件路径判断，不存在就创建文件夹；并在windows环境下命令调用openOffice的进程
 * 3.转换结束后，关闭连接和进程
 *
 */
public class OpenOfficePdfConvert {
    /**
     * OpenOfficeHome路径
     */
    private static String OPEN_OFFICE_HOME = "C:/Program Files (x86)/OpenOffice 4/program/";
    /**
     * 启动服务的命令
     */
    private static String COMMAND = "soffice -headless -accept=\"socket,host=127.0.0.1,port=8100;urp;\"";

    private static int NOT_EXIST = -2;

    private static int ALREADY_PDF =3 ;

    private static int SUCCESS = 1;

    private static int FAIL = -1;

    private static  String PDF_FORMAT = "pdf";

    private static boolean officeToPDF(String sourceFile, String destFile) {
/**
 * 启动服务
 */
        Process process  = startOpenOffice();
        OpenOfficeConnection connection =null;
        try {
            File inputFile = new File(sourceFile);
            if (!inputFile.exists()) {
                // 找不到源文件, 则返回false
                return false;
            }
            // 如果目标路径不存在, 则新建该路径
            File outputFile = new File(destFile);
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            //如果目标文件存在，则删除
            if (outputFile.exists()) {
                outputFile.delete();
            }
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm ss");
            connection = new SocketOpenOfficeConnection("127.0.0.1", 8100);
            connection.connect();
            //openOffice连接时间
            System.out.println("连接时间:" + df.format(new Date()));
            DocumentConverter converter = new StreamOpenOfficeDocumentConverter(
                    connection);
            converter.convert(inputFile, outputFile);
            //转PDF的转换时间
            System.out.println("转换时间:" + df.format(new Date()));
            return true;
        } catch (ConnectException e) {
            e.printStackTrace();
            System.err.println("openOffice连接失败！请检查IP,端口");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(connection!=null){
             connection.disconnect();
            }
            /**
             * 避免进程一直存在，执行结束后杀掉进程
             */
            if(process!=null){
                process.destroy();
            }
        }
        return false;
    }





    /**
     * 启动openOffice服务
     */
    private static Process startOpenOffice(){
        // 启动OpenOffice的服务的完整命令
        String fullCommand = OPEN_OFFICE_HOME + COMMAND;
        try {
            return Runtime.getRuntime().exec(fullCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将传入的文件转换为pdf格式
     * @param sourceFile 传入需要转换的文件的绝对路径
     * @param destFile   转换后的文件的路径和文件名称
     * @return -2 文件不存在
     *          -3 文件已经转换
     *          1   转换成功
     */
    public static int  convertFileToPdf(String sourceFile, String destFile) {
        String kind = getFileSubFix(sourceFile);
        File file = new File(sourceFile);
        // 文件不存在
        if (!file.exists()) {
            return NOT_EXIST;
        }
        // 原文件就是PDF文件
        if (PDF_FORMAT.equals(kind)) {
            return  ALREADY_PDF;
        }
        if(officeToPDF(sourceFile, destFile)){
            return SUCCESS;
        }else{
            return FAIL;
        }
    }



    /***
     * 判断文件类型
     *
     * @param fileName
     * @return
     */
    private static String getFileSubFix(String fileName) {
        int splitIndex = fileName.lastIndexOf(".");
        return fileName.substring(splitIndex + 1);
    }


public static void main(String[] args) {
     OpenOfficePdfConvert.convertFileToPdf("E:\\tools\\temp\\2019-02-21\\3f1f37b20b6a4c16a099afbd979cefaa.doc", "E:\\tools\\temp\\2019-02-21\\3f1f37b20b6a4c16a099afbd979cefaa.doc");
    }


}

