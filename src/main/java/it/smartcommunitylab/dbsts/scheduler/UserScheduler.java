package it.smartcommunitylab.dbsts.scheduler;


import it.smartcommunitylab.dbsts.service.interfaces.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UserScheduler {

    private final UserService userService;

    public UserScheduler(UserService userService) {
        this.userService = userService;
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void runUserCheckTask() {
        userService.delete();
    }
}
