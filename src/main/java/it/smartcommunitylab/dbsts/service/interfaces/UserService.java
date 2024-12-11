package it.smartcommunitylab.dbsts.service.interfaces;

import it.smartcommunitylab.dbsts.db.entity.User;

public interface UserService {

    User create(User user);
    void delete(String username);
}
