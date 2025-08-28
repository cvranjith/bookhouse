package com.cvr.bookhouse.security;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;
import org.springframework.security.access.AccessDeniedException;

import com.cvr.bookhouse.service.UserService;

@Configuration
@EnableMethodSecurity // enables @PreAuthorize, @Secured, etc.
public class SecurityConfig {

  @Bean
  public UserDetailsService userDetailsService(UserService userService) {
    return username -> userService.findUser(username)
        .map(UserPrincipal::new)
        .orElseThrow(() -> new UsernameNotFoundException(username));
  }
  
  
  @SuppressWarnings("deprecation")
  @Bean
    public AuthenticationManager authenticationManager(UserDetailsService uds) {
    DaoAuthenticationProvider p = new DaoAuthenticationProvider();
    p.setUserDetailsService(uds);
    p.setPasswordEncoder(NoOpPasswordEncoder.getInstance());
    return new ProviderManager(p);
  }
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public CommandExceptionResolver securityExceptions() {
    final String RED   = "\u001B[31m";
    final String RESET = "\u001B[0m";
    return ex -> {
      if (ex instanceof AuthenticationCredentialsNotFoundException) {
        return CommandHandlingResult.of( RED+"🔐 Please login first: login <userId>\n"+RESET, 1);
      }
      if (ex instanceof AccessDeniedException) {
        return CommandHandlingResult.of(RED+"🚫 Forbidden! User is not Admin\n"+RESET, 1);
      }
      return null; // let other resolvers handle
    };
  }
  
}
