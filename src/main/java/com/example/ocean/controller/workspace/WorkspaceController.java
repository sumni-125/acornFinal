package com.example.ocean.controller.workspace;

import com.example.ocean.domain.Workspace;
import com.example.ocean.domain.WorkspaceDept;
import com.example.ocean.domain.WorkspaceMember;
import com.example.ocean.service.WorkspaceService;
import com.example.ocean.security.oauth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;


// REST API 전용 컨트롤러 (JSON 응답)
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    // 워크스페이스 목록 조회
    @GetMapping
    public ResponseEntity<List<Workspace>> getMyWorkspaces(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<Workspace> workspaces = workspaceService
                .getWorkspacesByUserId(userPrincipal.getId());

        return ResponseEntity.ok(workspaces);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // "yyyy-MM-dd" 형식의 문자열을 Timestamp 타입으로 변환하도록 설정
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        binder.registerCustomEditor(Timestamp.class, new CustomDateEditor(dateFormat, true));
    }

    // 워크스페이스 생성
    @PostMapping
    public ResponseEntity<Workspace> createWorkspace(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @ModelAttribute Workspace workspace, // 2. 여기에 workspaceNm, endDate 등이 자동으로 담겨옵니다.
            @RequestParam(value = "departments", required = false) List<String> departments,
            @RequestParam(value = "workspaceImageFile", required = false) MultipartFile workspaceImg){
        try {
            // 3. 서비스 호출: 컨트롤러는 서비스에 데이터를 넘겨주기만.
            //    - UUID, 초대코드, 날짜 생성 등은 서비스에서 처리.
                // 컨트롤러는 서비스 호출만 담당.
                Workspace createdWorkspace = workspaceService.createWorkspace(
                        userPrincipal,
                        workspace,
                        departments,
                        workspaceImg
                );

                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(createdWorkspace);

            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
    }

    // 워크스페이스 상세 조회
    @GetMapping("/{workspaceCd}")
    public ResponseEntity<Map<String, Object>> getWorkspaceDetail(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Workspace workspace = workspaceService.findWorkspaceByCd(workspaceCd);
        if (workspace == null) {
            return ResponseEntity.notFound().build();
        }

        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspaceCd);

        Map<String, Object> response = new HashMap<>();
        response.put("workspace", workspace);
        response.put("members", members);

        return ResponseEntity.ok(response);
    }

    // 워크스페이스 프로필 조회
    @GetMapping("/{workspaceCd}/profile")
    public ResponseEntity<WorkspaceMember> getProfile(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        WorkspaceMember profile = workspaceService
                .findMemberByWorkspaceAndUser(workspaceCd, userPrincipal.getId());

        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(profile);
    }


    // 워크스페이스 사용자 프로필 수정
    @PutMapping("/{workspaceCd}/profile")
    public ResponseEntity<WorkspaceMember> updateProfile(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody WorkspaceMember profileData) {

        workspaceService.updateWorkspaceProfile(
                workspaceCd,
                userPrincipal.getId(),
                profileData.getUserNickname(),
                profileData.getStatusMsg(),
                profileData.getEmail(),
                profileData.getPhoneNum(),
                null // 이미지는 별도 처리
        );

        workspaceService.updateDeptAndPosition(
                workspaceCd,
                userPrincipal.getId(),
                profileData.getDeptCd(),
                profileData.getPosition()
        );

        WorkspaceMember updatedProfile = workspaceService
                .findMemberByWorkspaceAndUser(workspaceCd, userPrincipal.getId());

        return ResponseEntity.ok(updatedProfile);
    }

    // 부서 목록 조회
    @GetMapping("/{workspaceCd}/departments")
    public ResponseEntity<List<WorkspaceDept>> getDepartments(
            @PathVariable String workspaceCd) {

        List<WorkspaceDept> departments = workspaceService.getDepartments(workspaceCd);
        return ResponseEntity.ok(departments);
    }

    // 즐겨찾기 토글
    @PatchMapping("/{workspaceCd}/favorite")
    public ResponseEntity<Void> toggleFavorite(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, Boolean> request) {

        boolean isFavorite = request.get("favorite");
        workspaceService.updateFavoriteStatus(
                userPrincipal.getId(),
                Arrays.asList(workspaceCd),
                isFavorite
        );

        return ResponseEntity.ok().build();
    }

    // 워크스페이스 입장 시간 업데이트
    @PostMapping("/{workspaceCd}/enter")
    public ResponseEntity<Void> enterWorkspace(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        workspaceService.updateEntranceTime(workspaceCd, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    // 워크스페이스 퇴장 시간 업데이트
    @PostMapping("/{workspaceCd}/exit")
    public ResponseEntity<Void> exitWorkspace(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        workspaceService.updateQuitTime(workspaceCd, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    // 사용자 상태 업데이트
    @PatchMapping("/{workspaceCd}/user-state")
    public ResponseEntity<Void> updateUserState(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> request) {

        String userState = request.get("userState");
        workspaceService.updateUserState(workspaceCd, userPrincipal.getId(), userState);

        return ResponseEntity.ok().build();
    }

    // 이미지 업로드 (별도 엔드포인트)
    @PostMapping("/{workspaceCd}/profile/image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("image") MultipartFile image) throws IOException {

        if (image.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        File uploadDir = new File("C:/ocean_img");
        if (!uploadDir.exists()) uploadDir.mkdirs();

        String ext = image.getOriginalFilename()
                .substring(image.getOriginalFilename().lastIndexOf("."));
        String savedFileName = UUID.randomUUID() + ext;

        image.transferTo(new File(uploadDir, savedFileName));

        // DB 업데이트
        workspaceService.updateWorkspaceProfile(
                workspaceCd,
                userPrincipal.getId(),
                null, null, null, null,
                savedFileName
        );

        Map<String, String> response = new HashMap<>();
        response.put("imageUrl", savedFileName);

        return ResponseEntity.ok(response);
    }
}