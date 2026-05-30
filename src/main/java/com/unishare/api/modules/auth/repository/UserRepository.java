package com.unishare.api.modules.auth.repository;

import com.unishare.api.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

        Optional<User> findByEmail(String email);

        boolean existsByEmail(String email);

        /**
         * Tất cả user có role {@code roleName} (vd.
         * {@link com.unishare.api.common.constants.Roles#MENTOR}).
         */
        @Query("""
                        SELECT DISTINCT u FROM User u
                        JOIN FETCH u.userRoles ur
                        JOIN FETCH ur.role r
                        WHERE r.name = :roleName
                        """)
        List<User> findAllWithRoleName(@Param("roleName") String roleName);

        @Query("""
                        SELECT u FROM User u
                        JOIN FETCH u.userRoles ur
                        JOIN FETCH ur.role
                        WHERE u.id = :id
                        """)
        Optional<User> findByIdWithRoles(@Param("id") UUID id);

        Optional<User> findByPhoneNumber(String phoneNumber);

        boolean existsByPhoneNumber(String phoneNumber);
}
