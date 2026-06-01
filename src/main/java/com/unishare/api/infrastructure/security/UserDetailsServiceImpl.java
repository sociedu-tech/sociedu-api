package com.unishare.api.infrastructure.security;

import com.unishare.api.common.constants.UserStatuses;
import com.unishare.api.modules.auth.entity.User;
import com.unishare.api.modules.auth.entity.UserCredential;
import com.unishare.api.modules.auth.repository.UserCredentialRepository;
import com.unishare.api.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return buildPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        return buildPrincipal(user);
    }

    private CustomUserPrincipal buildPrincipal(User user) {
        UserCredential credential = userCredentialRepository.findByUserId(user.getId())
                .orElse(null);

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        boolean enabled = UserStatuses.ACTIVE.equalsIgnoreCase(user.getStatus())
                && Boolean.TRUE.equals(user.getEmailVerified());

        return new CustomUserPrincipal(
                user.getId(),
                user.getEmail(),
                credential != null ? credential.getPasswordHash() : null,
                roles,
                enabled
        );
    }
}
