package it.smartcommunitylab.dbsts.config;

import it.smartcommunitylab.dbsts.db.DbManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Autowired
    DbManager dbManager;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(50);
        scheduler.setThreadNamePrefix("scheduled-task-");
        return scheduler;
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void removeExpiredUsers() {
        dbManager.cleanupExpired();
    }
}
