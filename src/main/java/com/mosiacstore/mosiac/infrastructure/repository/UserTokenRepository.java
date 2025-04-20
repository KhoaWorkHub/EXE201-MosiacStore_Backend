package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.user.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

}
