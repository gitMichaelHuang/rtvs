package com.example.rtvs.security;

import com.example.rtvs.domain.UserAccount;
import com.example.rtvs.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        return User.builder()
                .username(account.getUserId())
                .password(account.getPassword())        // BCrypt-hashed at rest
                .authorities(List.of(new SimpleGrantedAuthority(account.getRole())))
                .accountLocked(account.getStatus().isBlocked())
                .build();
    }
}