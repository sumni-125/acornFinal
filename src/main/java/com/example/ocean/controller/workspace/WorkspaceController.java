package com.example.ocean.controller.workspace;

import com.example.ocean.domain.Workspace;
import com.example.ocean.domain.WorkspaceDept;
import com.example.ocean.domain.WorkspaceMember;
import com.example.ocean.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    @Autowired
    private WorkspaceService workspaceService;

    @GetMapping("/workspace/create")
    public String showCreateForm() {
        return "create-workspace"; // 워크스페이스 생성 폼
    }

    @PostMapping("/workspace/create")
    public String createWorkspace(@AuthenticationPrincipal OAuth2User user,
                                  @RequestParam("workspaceNm") String workspaceNm,
                                  @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                  @RequestParam(value = "workspaceImg", required = false) MultipartFile imageFile,
                                  @RequestParam("departments") String[] departments,
                                  RedirectAttributes redirectAttributes) throws IOException {

        String userId = user.getAttribute("sub");

        String workspaceCd = UUID.randomUUID().toString();
        String inviteCd = UUID.randomUUID().toString().substring(0, 8);

        String savedFileName = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            File uploadDir = new File("C:/ocean_img");
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String ext = imageFile.getOriginalFilename()
                    .substring(imageFile.getOriginalFilename().lastIndexOf("."));
            savedFileName = UUID.randomUUID().toString() + ext;
            File destination = new File(uploadDir, savedFileName);
            imageFile.transferTo(destination);
        }

        Workspace workspace = new Workspace();
        workspace.setWorkspaceCd(workspaceCd);
        workspace.setWorkspaceNm(workspaceNm);
        workspace.setInviteCd(inviteCd);
        workspace.setActiveState("1");
        workspace.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
        workspace.setEndDate(Timestamp.valueOf(endDate.atStartOfDay()));
        workspace.setWorkspaceImg(savedFileName);

        workspaceService.createWorkspaceWithDepartments(workspace, departments, userId);

        redirectAttributes.addFlashAttribute("message", "워크스페이스가 생성되었습니다.");
        return "redirect:/workspace/" + workspaceCd + "/set-profile";
    }

    @GetMapping("/workspace/{workspaceCd}/set-profile")
    public String showProfileForm(@PathVariable String workspaceCd,
                                  @AuthenticationPrincipal OAuth2User user,
                                  Model model) {
        String userId = user.getAttribute("sub");

        Workspace workspace = workspaceService.findWorkspaceByCd(workspaceCd);
        List<WorkspaceDept> departments = workspaceService.getDepartments(workspaceCd);

        model.addAttribute("workspaceCd", workspaceCd);
        model.addAttribute("departments", departments);
        model.addAttribute("userId", userId);
        model.addAttribute("workspace", workspace);

        return "set-profile";
    }

    @PostMapping("/workspace/{workspaceCd}/set-profile")
    public String submitProfile(@PathVariable String workspaceCd,
                                @AuthenticationPrincipal OAuth2User user,
                                @RequestParam String userNickname,
                                @RequestParam(required = false) String statusMsg,
                                @RequestParam String email,
                                @RequestParam(required = false) String phoneNum,
                                @RequestParam String deptCd,
                                @RequestParam String position,
                                @RequestParam(required = false) MultipartFile userImg,
                                RedirectAttributes redirectAttributes) throws IOException {

        String userId = user.getAttribute("sub");

        // 이미지 처리
        String savedFileName = null;
        if (userImg != null && !userImg.isEmpty()) {
            File uploadDir = new File("C:/ocean_img");
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String ext = userImg.getOriginalFilename().substring(userImg.getOriginalFilename().lastIndexOf("."));
            savedFileName = UUID.randomUUID() + ext;
            userImg.transferTo(new File(uploadDir, savedFileName));
        }

        workspaceService.updateWorkspaceProfile(workspaceCd, userId, userNickname, statusMsg, email, phoneNum, savedFileName);
        workspaceService.updateDeptAndPosition(workspaceCd, userId, deptCd, position);

        redirectAttributes.addFlashAttribute("message", "프로필이 설정되었습니다.");
        return "redirect:/workspace/" + workspaceCd + "/main";
    }

    @GetMapping("/workspace/enter/{workspaceCd}")
    public String enterWorkspace(@PathVariable String workspaceCd,
                                 @AuthenticationPrincipal OAuth2User user,
                                 RedirectAttributes redirectAttributes) {
        String userId = user.getAttribute("sub");

        String nickname = workspaceService.getUserNicknameInWorkspace(workspaceCd, userId);

        if (nickname == null || nickname.trim().isEmpty()) {
            return "redirect:/workspace/" + workspaceCd + "/set-profile";
        }

        return "redirect:/workspace/" + workspaceCd + "/main";
    }

    @GetMapping("/workspace/{workspaceCd}/main")
    public String workspaceMain(@PathVariable String workspaceCd,
                                @AuthenticationPrincipal OAuth2User user,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        String userId = user.getAttribute("sub");
        workspaceService.updateEntranceTime(workspaceCd, userId);

        Workspace workspace = workspaceService.findWorkspaceByCd(workspaceCd);
        if (workspace == null) {
            redirectAttributes.addFlashAttribute("error", "notfound");
            return "redirect:/workspace";
        }

        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspaceCd);

        model.addAttribute("workspace", workspace);
        model.addAttribute("members", members);
        model.addAttribute("loginUserId", userId);

        return "workspace-detail";
    }

    @PostMapping("/workspace/favorite")
    public String handleFavorite(
            @AuthenticationPrincipal OAuth2User user,
            @RequestParam(name = "selectedWorkspaces", required = false) List<String> selectedWorkspaces,
            @RequestParam String action,
            RedirectAttributes redirectAttributes) {

        String userId = user.getAttribute("sub");

        if (selectedWorkspaces != null && !selectedWorkspaces.isEmpty()) {
            if (action.equals("favorite")) {
                workspaceService.updateFavoriteStatus(userId, selectedWorkspaces, true);
            } else if (action.equals("unfavorite")) {
                workspaceService.updateFavoriteStatus(userId, selectedWorkspaces, false);
            }
        }

        redirectAttributes.addFlashAttribute("message", "처리되었습니다.");
        return "redirect:/workspace";
    }

    @GetMapping("/workspace/{workspaceCd}/exit")
    public String exitWorkspace(@PathVariable String workspaceCd,
                                @AuthenticationPrincipal OAuth2User user,
                                @RequestParam(required = false) String redirect) {
        String userId = user.getAttribute("sub");
        workspaceService.updateQuitTime(workspaceCd, userId);

        if ("main".equals(redirect)) {
            return "redirect:/";
        }
        return "redirect:/workspace";
    }

    @GetMapping("/workspace/{workspaceCd}/edit-profile")
    public String showEditProfileForm(@PathVariable String workspaceCd,
                                      @AuthenticationPrincipal OAuth2User user,
                                      Model model) {
        String userId = user.getAttribute("sub");
        WorkspaceMember profile = workspaceService.findMemberByWorkspaceAndUser(workspaceCd, userId);
        List<WorkspaceDept> depts = workspaceService.getDepartments(workspaceCd);

        model.addAttribute("profile", profile);
        model.addAttribute("depts", depts);
        return "edit-workspace-profile";
    }

    @PostMapping("/workspace/{workspaceCd}/edit-profile")
    public String updateProfile(@PathVariable String workspaceCd,
                                @AuthenticationPrincipal OAuth2User user,
                                @ModelAttribute WorkspaceMember profile,
                                @RequestParam(required = false) MultipartFile userImg) throws IOException {

        String userId = user.getAttribute("sub");
        profile.setUserId(userId);
        profile.setWorkspaceCd(workspaceCd);

        String savedFileName = null;
        if (userImg != null && !userImg.isEmpty()) {
            File uploadDir = new File("C:/ocean_img");
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String ext = userImg.getOriginalFilename().substring(userImg.getOriginalFilename().lastIndexOf("."));
            savedFileName = UUID.randomUUID() + ext;
            userImg.transferTo(new File(uploadDir, savedFileName));
        }

        workspaceService.updateWorkspaceProfile(
                workspaceCd, userId,
                profile.getUserNickname(),
                profile.getStatusMsg(),
                profile.getEmail(),
                profile.getPhoneNum(),
                savedFileName
        );

        workspaceService.updateDeptAndPosition(
                workspaceCd, userId,
                profile.getDeptCd(),
                profile.getPosition()
        );

        return "redirect:/workspace/" + workspaceCd + "/main";
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("userImg");
    }

    @PostMapping("/workspace/{workspaceCd}/user-state")
    @ResponseBody
    public ResponseEntity<String> updateUserState(@PathVariable String workspaceCd,
                                                  @AuthenticationPrincipal OAuth2User user,
                                                  @RequestBody Map<String, String> body) {
        String userId = user.getAttribute("sub");
        String userState = body.get("userState");

        workspaceService.updateUserState(workspaceCd, userId, userState);
        return ResponseEntity.ok("updated");
    }

}
