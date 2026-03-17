package com.example.rtvs.service;

import com.example.rtvs.domain.UserAccount;
import com.example.rtvs.dto.AuthRequest;
import com.example.rtvs.dto.AuthResponse;
import com.example.rtvs.repository.UserAccountRepository;
import com.example.rtvs.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccountRepository userAccountRepository;

    public AuthResponse authenticate(AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword()));

        String token = jwtTokenProvider.generateToken(auth);

        UserAccount account = userAccountRepository.findById(request.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + request.getUserId()));

        return AuthResponse.builder()
                .token(token)
                .userId(request.getUserId())
                .role(account.getRole())
                .build();
    }
}
