package ideapropiaprimero.security;
import ideapropiaprimero.repository.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return login -> {
            var account = users.findByLogin(login).orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
            return User.withUsername(account.getUsername()).password(account.getPasswordHash())
                    .roles(account.getRole().name()).build();
        };
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/register/submit", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                .requestMatchers("/admin", "/admin/**").hasRole("AUXILIAR")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/welcome", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").invalidateHttpSession(true)
                    .clearAuthentication(true).deleteCookies("JSESSIONID"));
        // CSRF y protección de fijación de sesión permanecen activadas.
        return http.build();
    }
}
