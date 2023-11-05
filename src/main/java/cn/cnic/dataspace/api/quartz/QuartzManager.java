package cn.cnic.dataspace.api.quartz;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;

public class QuartzManager {

    private static SchedulerFactory gSchedulerFactory = new StdSchedulerFactory();

    /**
     * @ Description: Add a scheduled task using the default task group name, trigger name, and trigger group name
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void addJob(String jobName, String group, Class cls, String cron) {
        try {
            Scheduler sched = gSchedulerFactory.getScheduler();
            JobDetail job = JobBuilder.newJob(cls).withIdentity(jobName, group).build();
            // Expression scheduling builder
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
            // Build a new trigger based on the new cronExpression expression
            Trigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName, group).withSchedule(scheduleBuilder).build();
            // Hand it over to the scheduler for scheduling
            sched.scheduleJob(job, trigger);
            // start-up
            if (!sched.isShutdown()) {
                sched.start();
                System.err.println("添加任务:" + jobName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // /**
    // *@ Description: Modify the task (you can modify the task name, task class, and trigger time)
    // *Principle: Remove the original task and add a new one
    // *@ param oldJobName: Original task name
    // * @param jobName
    // * @param jobclass
    // * @param cron
    // *@ date May 23, 2018 9:13:10 AM
    // */
    // @SuppressWarnings({ "rawtypes", "unchecked" })
    // public static void modifyJob(String oldJobName, String jobName, Class jobclass, String cron) {
    // /*
    // * removeJob(oldJobName);
    // * addJob(jobName, jobclass, cron);
    // *System. err. println ("Modify Task"+oldJobName);
    // */
    // TriggerKey triggerKey = TriggerKey.triggerKey(oldJobName, TRIGGER_GROUP_NAME);
    // JobKey jobKey = JobKey.jobKey(oldJobName, JOB_GROUP_NAME);
    // try {
    // Scheduler sched = gSchedulerFactory.getScheduler();
    // Trigger trigger = (Trigger) sched.getTrigger(triggerKey);
    // if (trigger == null) {
    // return;
    // }
    // Sched. pauseTrigger (triggerKey)// Stop trigger
    // Sched. unscheduleJob (triggerKey)// Remove Trigger
    // Sched. deleteJob (jobKey)// Delete Task
    // System. err. println ("Remove Task:"+oldJobName);
    // 
    // JobDetail job = JobBuilder.newJob(jobclass).withIdentity(jobName, JOB_GROUP_NAME).build();
    // //Expression scheduling builder
    // CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
    // //Build a new trigger based on the new cronExpression expression
    // Trigger newTrigger = TriggerBuilder.newTrigger().withIdentity(jobName, TRIGGER_GROUP_NAME)
    // .withSchedule(scheduleBuilder).build();
    // 
    // //Hand it over to the scheduler for scheduling
    // sched.scheduleJob(job, newTrigger);
    // 
    // //Start
    // if (!sched.isShutdown()) {
    // sched.start();
    // System. err. println ("Add new task:"+jobName);
    // }
    // System. err. println ("Modify task ["+oldJobName+"] to:"+jobName);
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // }
    // 
    // }
    /**
     * @ Description: Modify the trigger time of a task (using the default task group name, trigger name, and trigger group name)
     */
    public static void modifyJobTime(String jobName, String group, String cron) {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, group);
        try {
            Scheduler sched = gSchedulerFactory.getScheduler();
            CronTrigger trigger = (CronTrigger) sched.getTrigger(triggerKey);
            if (trigger == null) {
                return;
            }
            String oldTime = trigger.getCronExpression();
            if (!oldTime.equalsIgnoreCase(cron)) {
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
                // Rebuild trigger with new cronExpression expression
                trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
                // Press the new trigger to reset the job execution
                sched.rescheduleJob(triggerKey, trigger);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // /**
    // *@ Description: Modify the trigger time of a task
    // * @param triggerName
    // * @param triggerGroupName
    // * @param cron
    // */
    // public static void modifyJobTime(String triggerName, String triggerGroupName, String cron) {
    // TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroupName);
    // try {
    // Scheduler sched = gSchedulerFactory.getScheduler();
    // CronTrigger trigger = (CronTrigger) sched.getTrigger(triggerKey);
    // if (trigger == null) {
    // return;
    // }
    // String oldTime = trigger.getCronExpression();
    // if (!oldTime.equalsIgnoreCase(cron)) {
    // //If the trigger already exists, update the corresponding timing settings
    // CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
    // //Rebuild trigger with new cronExpression expression
    // trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
    // //Press the new trigger to reset the job execution
    // sched.resumeTrigger(triggerKey);
    // }
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // }
    // }
    /**
     * Remove a task (using default task group name, trigger name, trigger group name)
     */
    public static void removeJob(String jobName, String group) {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, group);
        JobKey jobKey = JobKey.jobKey(jobName, group);
        try {
            Scheduler sched = gSchedulerFactory.getScheduler();
            Trigger trigger = sched.getTrigger(triggerKey);
            if (trigger == null) {
                return;
            }
            // Stop trigger
            sched.pauseTrigger(triggerKey);
            // Remove Trigger
            sched.unscheduleJob(triggerKey);
            // Delete Task
            sched.deleteJob(jobKey);
            System.err.println("移除任务:" + jobName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // /**
    // *@ Description: Remove a task
    // * @param jobName
    // * @param jobGroupName
    // * @param triggerName
    // * @param triggerGroupName
    // */
    // public static void removeJob(String jobName, String jobGroupName, String triggerName, String triggerGroupName) {
    // TriggerKey triggerKey = TriggerKey.triggerKey(jobName, triggerGroupName);
    // JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
    // try {
    // Scheduler sched = gSchedulerFactory.getScheduler();
    // Sched. pauseTrigger (triggerKey)// Stop trigger
    // Sched. unscheduleJob (triggerKey)// Remove Trigger
    // Sched. deleteJob (jobKey)// Delete Task
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // }
    // }
    /**
     * @ Description: Pause a task
     */
    public static void pauseJob(String jobName, String jobGroupName) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
        try {
            Scheduler sched = gSchedulerFactory.getScheduler();
            sched.pauseJob(jobKey);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * @ Description: Restore a task
     */
    public static void resumeJob(String jobName, String jobGroupName) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
        try {
            Scheduler sched = gSchedulerFactory.getScheduler();
            sched.resumeJob(jobKey);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * @ Description: Start all scheduled tasks
     */
    public static void startJobs() {
        try {
            Scheduler sched = gSchedulerFactory.getScheduler();
            sched.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @ Description: Close all scheduled tasks
     */
    public static void shutdownJobs() {
        try {
            Scheduler sched = gSchedulerFactory.getScheduler();
            if (!sched.isShutdown()) {
                sched.shutdown();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @ Description: Run the task immediately. The 'Run Now' option here only runs once, making it convenient for testing purposes.
     */
    public static void triggerJob(String jobName, String jobGroupName) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
        try {
            Scheduler sched = gSchedulerFactory.getScheduler();
            sched.triggerJob(jobKey);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * @ Description: Get Task Status
     */
    public static String getTriggerState(String jobName, String jobGroupName) {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroupName);
        String name = null;
        try {
            Scheduler sched = gSchedulerFactory.getScheduler();
            TriggerState triggerState = sched.getTriggerState(triggerKey);
            name = triggerState.name();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        return name;
    }

    /**
     * @ Description: Obtain the last 8 execution times
     */
    public static List<String> getRecentTriggerTime(String cron) {
        List<String> list = new ArrayList<String>();
        try {
            CronTriggerImpl cronTriggerImpl = new CronTriggerImpl();
            cronTriggerImpl.setCronExpression(cron);
            // This is the key point, one line of code will handle it
            List<Date> dates = TriggerUtils.computeFireTimes(cronTriggerImpl, null, 8);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (Date date : dates) {
                list.add(dateFormat.format(date));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return list;
    }
}
