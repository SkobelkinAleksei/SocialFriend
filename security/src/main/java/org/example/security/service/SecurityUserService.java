package org.example.security.service;

import lombok.RequiredArgsConstructor;
import org.example.eventskafka.UserRegisteredEvent;
import org.example.security.entity.SecurityUserDetails;
import org.example.security.entity.UserSecurity;
import org.example.security.repository.UserSecurityRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class SecurityUserService implements UserDetailsService {

    private final UserSecurityRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserSecurity user = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Такой пользователь не был найден: " + username));

        return new SecurityUserDetails(user);
    }

    public void createUserFromEvent(UserRegisteredEvent event) {
        UserSecurity userSecurity = new UserSecurity();
        userSecurity.setId(event.getId());
        userSecurity.setUsername(event.getUsername());
        userSecurity.setPassword(event.getPasswordHash());

        repository.save(userSecurity);
    }
}
