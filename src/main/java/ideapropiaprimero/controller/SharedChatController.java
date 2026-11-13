package ideapropiaprimero.controller;

import ideapropiaprimero.repository.ShareRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class SharedChatController {
    private final ShareRepository shares;
    public SharedChatController(ShareRepository shares){this.shares=shares;}
    @GetMapping("/share/{token}")
    public String view(@PathVariable String token,Model model) {
        model.addAttribute("shared",shares.find(token).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "chat/shared";
    }
}
