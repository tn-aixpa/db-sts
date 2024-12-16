package it.smartcommunitylab.dbsts.service;

import it.smartcommunitylab.dbsts.db.entity.User;
import it.smartcommunitylab.dbsts.db.repository.UserRepository;
import it.smartcommunitylab.dbsts.service.interfaces.UserService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User create(User user) {
        return userRepository.save(user);
    }

    @Override
    public void delete() {

        List<User> expiredUsers = userRepository.findExpiredUsers(Instant.now());

        if (!expiredUsers.isEmpty()) {
            List<Long> ids = expiredUsers.stream().map(User::getId).toList();
            userRepository.markUsersAsDeleted(ids);
        }
    }
}
