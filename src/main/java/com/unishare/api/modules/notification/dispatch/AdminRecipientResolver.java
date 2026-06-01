package com.unishare.api.modules.notification.dispatch;

import com.unishare.api.common.constants.Roles;
import com.unishare.api.modules.auth.entity.User;
import com.unishare.api.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminRecipientResolver {

    private final UserRepository userRepository;

    public List<UUID> findAdminUserIds() {
        return userRepository.findAllWithRoleName(Roles.ADMIN).stream()
                .map(User::getId)
                .toList();
    }
}
