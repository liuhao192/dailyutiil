package ren.kura.utils;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.List;

/**
 * @author: liuha
 * @Date: 2018/8/21
 * @Time: 11:31
 * @Description: 保证项目启动或重启时, 所有任务会被重新安排到任务调度中.
 */
public class ScheduleStartListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
           recovery();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    public void recovery() {

        Scheduler scheduler = null;

        try {

            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            //可以通过SchedulerFactory创建一个Scheduler实例
            scheduler = schedulerFactory.getScheduler();
            //获取调度器中所有的触发器组
            List<String> triggerGroups = scheduler.getTriggerGroupNames();
            System.out.println("调度器中所有的触发器组 size():" + triggerGroups.size());
           //重新恢复在triggerGroups组中所有的触发器
            if (triggerGroups != null && triggerGroups.size() != 0) {
                for (int i = 0; i < triggerGroups.size(); i++) {
                    TriggerKey triggerKey = TriggerKey.triggerKey(triggerGroups.get(i), triggerGroups.get(i));
                    System.out.println("triggerKey:" + triggerKey);
           //获取trigger
                    Trigger tg = scheduler.getTrigger(triggerKey);
                    System.out.println(triggerKey + " -> 执行时间 :" + tg.getNextFireTime());
           //按新的trigger重新设置job执行
                    scheduler.rescheduleJob(triggerKey, tg);
                }
            }
            scheduler.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
