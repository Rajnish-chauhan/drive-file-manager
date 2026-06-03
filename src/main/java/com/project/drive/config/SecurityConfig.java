package com.project.drive.config;

import com.project.drive.entity.UserEntity;
import com.project.drive.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // Spring ke default fake logins close
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())

                .authorizeHttpRequests(auth -> auth
                        // FIX: Sirf API paths allow hote hain yahan, full URL nahi
                        .requestMatchers("/api/auth/me", "/api/auth/login", "/api/auth/register").permitAll()
                        .anyRequest().authenticated()
                )

                // OAuth2 login data save in DB
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorizationEndpoint ->
                                authorizationEndpoint.authorizationRequestResolver(
                                        authorizationRequestResolver(this.clientRegistrationRepository)
                                )
                        )
                        .successHandler((request, response, authentication) -> {

                            // 1. Google/GitHub user details
                            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
                            String email = oauthUser.getAttribute("email");
                            String name = oauthUser.getAttribute("name");

                            // Fallback for GitHub
                            if (email == null) email = oauthUser.getAttribute("login") + "@github.com";
                            if (name == null) name = oauthUser.getAttribute("login");

                            // 2. Database check
                            Optional<UserEntity> existingUser = userRepository.findByEmail(email);

                            if (existingUser.isEmpty()) {
                                // 3. if new user then insert in DB
                                UserEntity newUser = new UserEntity();
                                newUser.setEmail(email);
                                newUser.setName(name);
                                newUser.setPassword("OAUTH_" + UUID.randomUUID().toString().substring(0,8));

                                userRepository.save(newUser);
                                System.out.println("NEW USER SAVED: " + email);
                            } else {
                                System.out.println("EXISTING USER: " + email);
                            }

                            // 4. LIVE FRONTEND REDIRECT (Vercel URL)
                            // Agar local testing karni ho toh isko temporarily "http://localhost:5173" kar dena
                            response.sendRedirect("https://drive.rajnishsystems.in");
                        })
                )
                .logout(logout -> logout
                        // LIVE FRONTEND LOGOUT REDIRECT
                        .logoutSuccessUrl("https://drive.rajnishsystems.in")
                        .deleteCookies("JSESSIONID")
                );
        return http.build();
    }

    // --- METHODS FOR FORCING CONSENT SCREEN ---

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {

        DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");

        authorizationRequestResolver.setAuthorizationRequestCustomizer(
                authorizationRequestCustomizer());

        return authorizationRequestResolver;
    }

    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer() {
        return customizer -> customizer
                .additionalParameters(params -> params.put("prompt", "consent"));
    }

    // ----------------------------------------------
    // GLOBAL CORS FIX FOR PRODUCTION
    // ----------------------------------------------
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Dono URLs allow kiye hain: Live Vercel wala aur Local testing wala
        configuration.setAllowedOrigins(List.of(
                "https://drive.rajnishsystems.in",
                "http://localhost:5173"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));

        // Credentials (Cookies/Sessions) allow karne ke liye true hona lazmi hai
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}