package ideapropiaprimero.controller;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {
    @GetMapping("/welcome") public String welcome(Principal principal, Authentication auth, Model model) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("auxiliar", auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_AUXILIAR")));
        return "auth/welcome";
    }
    @GetMapping("/admin") public String admin() { return "admin/index"; }
}
