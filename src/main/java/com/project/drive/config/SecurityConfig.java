package com.project.drive.config;

import com.project.drive.entity.UserEntity;
import com.project.drive.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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

    private final UserRepository userRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;

    // DYNAMIC FRONTEND URL INJECTION (Defaults to localhost:5173 if not found)
    @Value("${APP_BASE_URL_TEST}")
    private String frontendUrl;

    public SecurityConfig(UserRepository userRepository, ClientRegistrationRepository clientRegistrationRepository) {
        this.userRepository = userRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())

                // Prevents auto-redirects to OAuth for manual API calls
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/verify-otp",
                                "/api/files/public/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authEndpoint ->
                                authEndpoint.authorizationRequestResolver(
                                        authorizationRequestResolver(this.clientRegistrationRepository)
                                )
                        )
                        .successHandler((request, response, authentication) -> {
                            try {
                                OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
                                String email = oauthUser.getAttribute("email");
                                String name = oauthUser.getAttribute("name");
                                String login = oauthUser.getAttribute("login");

                                if (email == null) email = (login != null ? login : "user") + "@github.com";
                                if (name == null) name = (login != null ? login : "OAuth User");

                                Optional<UserEntity> existingUser = userRepository.findByEmail(email);

                                if (existingUser.isEmpty()) {
                                    UserEntity newUser = new UserEntity();
                                    newUser.setEmail(email);
                                    newUser.setName(name);
                                    newUser.setPassword(passwordEncoder().encode("OAUTH_" + UUID.randomUUID().toString().substring(0, 8)));
                                    newUser.setVerified(true);
                                    userRepository.save(newUser);
                                }

                                // DYNAMIC SUCCESS REDIRECT
                                response.sendRedirect(frontendUrl);

                            } catch (Exception e) {
                                e.printStackTrace();
                                response.sendRedirect(frontendUrl + "/?error=backend_crash");
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            System.err.println("OAuth2 Failure: " + exception.getMessage());
                            // DYNAMIC FAILURE REDIRECT
                            response.sendRedirect(frontendUrl + "/?error=oauth_failed");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl(frontendUrl) // DYNAMIC LOGOUT REDIRECT
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                );
        return http.build();
    }

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(authorizationRequestCustomizer());
        return resolver;
    }

    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer() {
        return customizer -> customizer.attributes(attributes -> {
            if ("google".equals(attributes.get(org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REGISTRATION_ID))) {
                customizer.additionalParameters(params -> params.put("prompt", "consent"));
            }
        });
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // DYNAMIC CORS ALLOWED ORIGIN (Strips trailing slash if present)
        String cleanFrontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;

        // ✅ NAYA CHANGE: Sirf cleanFrontendUrl pass kiya hai
        configuration.setAllowedOrigins(List.of(cleanFrontendUrl));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}