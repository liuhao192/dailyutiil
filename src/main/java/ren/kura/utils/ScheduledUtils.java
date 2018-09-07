package ren.kura.utils;


import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

/**
 * @author: liuha
 * @Date: 2018/8/21
 * @Time: 11:11
 * @Description: 这个类用于管理定时器的配置
 */
public class ScheduledUtils {
    //日志对象
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private SchedulerFactory schedulerFactory = new StdSchedulerFactory();

    /**
     * 将时间转换为cron 字符串
     *
     * @param Date 定时器启动的时间
     * @return
     */
    private String getCron(Date Date) {
        String cron = "0";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Date);
        cron += " " + calendar.get(Calendar.MINUTE);
        cron += " " + calendar.get(Calendar.HOUR_OF_DAY);
        cron += " " + calendar.get(Calendar.DAY_OF_MONTH);
        cron += " " + (calendar.get(Calendar.MONTH) + 1);
        cron += " ?";
        cron += " " + calendar.get(Calendar.YEAR);
        return cron;
    }

    /**
     * @param jobName          任务名
     * @param jobGroupName     任务组名
     * @param triggerName      触发器名
     * @param triggerGroupName 触发器组名
     * @param jobClass         触发时需要操作的类
     * @param startDate        触发时间  时间的格式为  yyyy-MM-dd HH:mm:ss
     * @param jobId            传入的参数
     * @Description: 添加一个定时任务
     * jobName jobGroupName  triggerName triggerGroupName  都要唯一 同一个定时器四者的名字可以一致
     */
    public void addJob(String jobName, String jobGroupName,
                       String triggerName, String triggerGroupName, Class jobClass, Date startDate, String jobId) throws com.chq.qft.inspect.exception.SchedulerException {

        try {
            Scheduler sched = schedulerFactory.getScheduler();
            // 任务名，任务组，任务执行类
            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName).build();
            jobDetail.getJobDataMap().put("jobId", jobId);
            // 触发器
            TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
            // 触发器名,触发器组
            triggerBuilder.withIdentity(triggerName, triggerGroupName);
            triggerBuilder.startNow();
            /**
             * 设置定时器为过期后 以当前时间为触发频率立刻触发一次执行
             */
            CronScheduleBuilder csb = CronScheduleBuilder.cronSchedule(getCron(startDate));
            csb.withMisfireHandlingInstructionIgnoreMisfires();
            // 触发器时间设定
            triggerBuilder.withSchedule(csb);
            // 创建Trigger对象
            CronTrigger trigger = (CronTrigger) triggerBuilder.build();
            // 调度容器设置JobDetail和Trigger
            sched.scheduleJob(jobDetail, trigger);
            // 启动
            if (!sched.isShutdown()) {
                sched.start();
            }
        } catch (Exception e) {
            //将之前的错误转为运行期的错误便于spring的回滚事件
            throw new com.chq.qft.inspect.exception.SchedulerException("addJob  error :" + e.getMessage());
        }

    }

    /**
     * @param jobName          任务名
     * @param jobGroupName     任务组名
     * @param triggerName      触发器名
     * @param triggerGroupName 触发器组名
     * @param updateDate       更新后的触发时间
     * @Description: 修改一个任务的触发时间
     */
    public void modifyJobTime(String jobName,
                              String jobGroupName, String triggerName, String triggerGroupName, Date updateDate) throws com.chq.qft.inspect.exception.SchedulerException {
        try {
            Scheduler sched = schedulerFactory.getScheduler();
            TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroupName);
            CronTrigger trigger = (CronTrigger) sched.getTrigger(triggerKey);
            if (trigger == null) {
                return;
            }
            String cron = getCron(updateDate);
            String oldTime = trigger.getCronExpression();
            if (!oldTime.equalsIgnoreCase(cron)) {
                /** 方式一 ：调用 rescheduleJob 开始 */
                // 触发器
                TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
                // 触发器名,触发器组
                triggerBuilder.withIdentity(triggerName, triggerGroupName);
                triggerBuilder.startNow();
                /**
                 * 设置定时器为过期后 以当前时间为触发频率立刻触发一次执行
                 */
                CronScheduleBuilder csb = CronScheduleBuilder.cronSchedule(cron);
                csb.withMisfireHandlingInstructionIgnoreMisfires();
                // 触发器时间设定
                triggerBuilder.withSchedule(csb);
                // 创建Trigger对象
                trigger = (CronTrigger) triggerBuilder.build();
                // 方式一 ：修改一个任务的触发时间
                sched.rescheduleJob(triggerKey, trigger);
                /** 方式一 ：调用 rescheduleJob 结束 */

                /** 方式二：先删除，然后在创建一个新的Job  */
                //JobDetail jobDetail = sched.getJobDetail(JobKey.jobKey(jobName, jobGroupName));
                //Class<? extends Job> jobClass = jobDetail.getJobClass();
                //removeJob(jobName, jobGroupName, triggerName, triggerGroupName);
                //addJob(jobName, jobGroupName, triggerName, triggerGroupName, jobClass, cron);
                /** 方式二 ：先删除，然后在创建一个新的Job */
            }
        } catch (Exception e) {
            //将之前的错误转为运行期的错误便于spring的回滚事件
            throw new com.chq.qft.inspect.exception.SchedulerException("modifyJobTime  error :" + e.getMessage());
        }
    }

    /**
     * @param jobName          任务名
     * @param jobGroupName     任务组名
     * @param triggerName      触发器名
     * @param triggerGroupName 触发器组名
     * @Description: 移除一个任务
     */
    public void removeJob(String jobName, String jobGroupName,
                          String triggerName, String triggerGroupName) throws com.chq.qft.inspect.exception.SchedulerException {
        try {
            Scheduler sched = schedulerFactory.getScheduler();
            TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroupName);
            sched.pauseTrigger(triggerKey);// 停止触发器
            sched.unscheduleJob(triggerKey);// 移除触发器
            sched.deleteJob(JobKey.jobKey(jobName, jobGroupName));// 删除任务
        } catch (Exception e) {
            throw new com.chq.qft.inspect.exception.SchedulerException("removeJob  error :" + e.getMessage());
        }
    }


    /**
     * @Description:启动所有定时任务 public  void startJobs() {
    try {
    Scheduler sched = schedulerFactory.getScheduler();
    sched.start();
    } catch (Exception e) {
    throw new RuntimeException(e);
    }
    }
     */
    /**
     * @Description:关闭所有定时任务 public  void shutdownJobs() {
    try {
    Scheduler sched = schedulerFactory.getScheduler();
    if (!sched.isShutdown()) {
    sched.shutdown();
    }
    } catch (Exception e) {
    throw new RuntimeException(e);
    }
    }
     */
}
