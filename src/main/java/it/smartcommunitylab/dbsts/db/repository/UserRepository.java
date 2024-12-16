package it.smartcommunitylab.dbsts.db.repository;

import it.smartcommunitylab.dbsts.db.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {

    @Query("SELECT u FROM User u WHERE u.dBUserValidUntil < :currentTime AND u.deleted = false")
    List<User> findExpiredUsers(Instant currentTime);

    @Modifying
    @Query("UPDATE User u SET u.deleted = true WHERE u.id IN :ids")
    void markUsersAsDeleted(List<Long> ids);
}

