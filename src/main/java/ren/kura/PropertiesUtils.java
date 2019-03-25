package ren.kura;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * @author liuha
 * Spring初始化的时候，会将system_config.properties配置文件内的key-value存入静态变量
 * 由于静态变量存在缓存中，读取速度较快
 * 也提供了重新更新值的方法
 */
@Component
public class PropertiesUtils {

	private static Properties prop;
    private static InputStream in;
    private static String PROPERTIES_NAME="system_config.properties";
    /**
     * 初始化配置的数据
     */
    public PropertiesUtils() {
        prop=new  Properties();
        in = PropertiesUtils.class.getClassLoader().getResourceAsStream(PROPERTIES_NAME);
        try {
            prop.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印全部的值信息
     */
   public static void printAllProperty()
    {
        Enumeration en = prop.propertyNames();
        while (en.hasMoreElements())
        {
            String key = (String) en.nextElement();
            String value = prop.getProperty(key);
            System.out.println(key + " : " + value);
        }
    }


    /**
     * 重新加载配置的之前，覆盖之前的缓存记录
     */
    public static void reLoad(){
       new PropertiesUtils();
    }

    /**
     * 查询某个key对应的值
     * @param key 需要查询的key值
     * @return value 对应的值
     */
    public static String getProValue(String key){
        return prop.getProperty(key);
    }

   /* public static void main(String[] args) throws IOException {
        PropertiesUtils.getPro("quarterTime");
    }*/


}


