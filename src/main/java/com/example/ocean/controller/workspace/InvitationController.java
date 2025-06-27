package com.example.ocean.controller.workspace;

import com.example.ocean.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
/*
    TODO ---> 워크스페이스 초대 관한 클래스
 */

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    @Autowired
    private WorkspaceService workspaceService;

    @GetMapping("/join")
    public String showJoinForm() {
        return "join-workspace"; // templates/join-workspace.html
    }

    @PostMapping("/request")
    public String requestInvitation(@AuthenticationPrincipal OAuth2User user,
                                    @RequestParam("inviteCd") String inviteCd,
                                    RedirectAttributes redirectAttributes) {
        String userId = user.getAttribute("sub");

        var workspace = workspaceService.findByInviteCode(inviteCd);
        if (workspace == null) {
            redirectAttributes.addFlashAttribute("message", "유효하지 않은 초대 코드입니다.");
            return "redirect:/invitations/join";
        }

        boolean already = workspaceService.existsInvitation(workspace.getWorkspaceCd(), userId);
        if (already) {
            redirectAttributes.addFlashAttribute("message", "이미 참가 요청을 보냈습니다.");
            return "redirect:/invitations/join";
        }

        workspaceService.requestInvitation(workspace.getWorkspaceCd(), userId, inviteCd);
        redirectAttributes.addFlashAttribute("message", "참가 요청이 전송되었습니다.");
        return "redirect:/workspace";
    }

    @PostMapping("/approve")
    public String approve(@RequestParam String workspaceCd,
                          @RequestParam String invitedUserId) {
        workspaceService.approveInvitation(workspaceCd, invitedUserId);
        return "redirect:/";
    }

    @PostMapping("/reject")
    public String reject(@RequestParam String workspaceCd,
                         @RequestParam String invitedUserId) {
        workspaceService.rejectInvitation(workspaceCd, invitedUserId);
        return "redirect:/";
    }

}
