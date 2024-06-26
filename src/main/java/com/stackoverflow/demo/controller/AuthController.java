package com.stackoverflow.demo.controller;

import com.stackoverflow.demo.model.LoginRequest;
import com.stackoverflow.demo.model.LoginResponse;
import com.stackoverflow.demo.securingweb.JWTIssuer;
import com.stackoverflow.demo.securingweb.UserPrincipal;
import com.stackoverflow.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final JWTIssuer jwtIssuer;

    @Autowired
    private UserService userService;

    @Autowired
    private final AuthenticationManager authenticationManager;

    @GetMapping("/secured")
    public String secured(@AuthenticationPrincipal UserPrincipal principal){
        return principal.getUsername() +"\n" + principal.getUserId() + "\n";
    }



    @PostMapping("/auth/login")
    public LoginResponse login(@RequestBody @Validated LoginRequest request){

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(),request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        var principal = (UserPrincipal) authentication.getPrincipal();

        var roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        var token = jwtIssuer.issue(principal.getUserId(),principal.getUsername(),roles);

        if (this.userService.getBannedStatus(principal.getUserId())){
            return LoginResponse.builder()
                    .accessToken("")
                    .banned(Boolean.TRUE)
                    .build();
        }

        return LoginResponse.builder()
                .accessToken(token)
                .banned(Boolean.FALSE)
                .build();
    }
}
