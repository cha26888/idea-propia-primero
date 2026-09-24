package ideapropiaprimero.controller;
import ideapropiaprimero.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {
    private final UserRepository users;
    public AdminController(UserRepository users) { this.users = users; }
    @GetMapping("/admin") public String getStudents(Model model) {
        model.addAttribute("students", users.findAllStudents());
        return "admin/index";
    }
}
