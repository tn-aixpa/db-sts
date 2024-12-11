package it.smartcommunitylab.dbsts.db.repository;

import it.smartcommunitylab.dbsts.db.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {}

