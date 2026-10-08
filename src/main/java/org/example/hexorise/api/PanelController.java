package org.example.hexorise.api;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class PanelController {
    @GetMapping({"/", "/manage", "/manage/"})
    public String panel() { return "redirect:/manage/index.html"; }
}
