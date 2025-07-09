function formatSecondsToHHMMSS(seconds) {
    const h = String(Math.floor(seconds / 3600)).padStart(2, '0');
    const m = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0');
    const s = String(seconds % 60).padStart(2, '0');
    return `${h}시간 ${m}분 ${s}초`;
}

document.addEventListener("DOMContentLoaded", function () {
    const userId = localStorage.getItem("userId");
    const workspaceCd = localStorage.getItem("workspaceCd");

    console.log("📦 로딩 시작 - userId:", userId, ", workspaceCd:", workspaceCd);

    if (!userId || !workspaceCd) {
        console.warn("⚠️ userId 또는 workspaceCd가 localStorage에 없습니다.");
        return;
    }

        // ✅ 참가 요청 목록 (owner만)
        if (workspaceCd && userId) {
            fetch(`/api/workspaces/${workspaceCd}/member/${userId}`)
                .then(res => res.json())
                .then(user => {
                    // 만약 owner라면 요청 불러오기
                    if (user.userRole === 'OWNER') {
                        fetch(`/api/workspaces/${workspaceCd}/invitations/pending`)
                            .then(res => res.json())
                            .then(pendingList => {
                                const box = document.getElementById("invitation-requests-box");
                                const list = document.getElementById("invitation-requests-list");
                                list.innerHTML = "";

                                if (pendingList.length === 0) {
                                    list.innerHTML = "<li>요청이 없습니다.</li>";
                                    return;
                                }

                                box.style.display = "block";

                                pendingList.forEach(req => {
                                console.log("💬 받은 초대 요청 객체:", req);
                                    const li = document.createElement("li");
                                    li.style.marginBottom = "10px";

                                    li.innerHTML = `
                                      <strong>${req.userName}</strong>님이 워크스페이스에 참가 요청을 보냈습니다
                                      <button onclick="respondToInvite('${req.INVITED_USER_ID}', 'ACCEPT')">수락</button>
                                      <button onclick="respondToInvite('${req.INVITED_USER_ID}', 'REJECT')">거절</button>
                                    `;
                                    list.appendChild(li);
                                });

                            });
                    }
                });
        }


    fetch(`/api/workspaces/${workspaceCd}/notifications`)
        .then(res => res.json())
        .then(notifications => {
            const list = document.getElementById("recent-notifications");
            list.innerHTML = "";

            if (!notifications || notifications.length === 0) {
                list.innerHTML = "<li>최근 알림이 없습니다.</li>";
                return;
            }

            notifications.forEach(noti => {
                const text = generateNotificationMessage(noti);
                const li = document.createElement("li");
                li.textContent = text;
                list.appendChild(li);
            });
        })
        .catch(err => {
            console.error("❌ 알림 불러오기 실패:", err);
            document.getElementById("recent-notifications").innerHTML = "<li>불러오기 실패</li>";
        });


    // ✅ 누적 접속 시간 로딩
    fetch(`/api/events/${workspaceCd}/usage-time`)
        .then(res => res.json())
        .then(seconds => {
            const formatted = formatSecondsToHHMMSS(seconds);
            document.getElementById("usage-time").textContent = formatted;
        })
        .catch(err => {
            console.error("사용 시간 로드 실패:", err);
            document.getElementById("usage-time").textContent = "불러오기 실패";
        });

    // ✅ 오늘 일정 불러오기
    fetch(`/api/events/today?userId=${userId}&workspaceCd=${workspaceCd}`)
        .then(response => response.json())
        .then(data => {
            const list = document.getElementById("user-events-list");
            list.innerHTML = "";

            if (!data || data.length === 0) {
                list.innerHTML = "<li>오늘 등록된 일정이 없습니다.</li>";
                return;
            }

            data.forEach(event => {
                const start = new Date(event.startDatetime);
                const time = start.toLocaleTimeString('ko-KR', {
                    hour: '2-digit',
                    minute: '2-digit'
                });

                const li = document.createElement("li");
                li.innerHTML = `<strong>${time}</strong> - ${event.title}`;
                list.appendChild(li);
            });
        })
        .catch(error => {
            console.error("❗ 오늘 일정 오류:", error);
            document.getElementById("user-events-list").innerHTML = "<li>일정 정보를 불러올 수 없습니다.</li>";
        });

    // ✅ 이번 주 완료 일정
    fetch(`/api/events/this-week-completed-count?workspaceCd=${workspaceCd}`)
        .then(res => res.json())
        .then(count => {
            document.getElementById('completed-this-week').innerText = `총 ${count}개 완료됨`;
        })
        .catch(err => {
            document.getElementById('completed-this-week').innerText = '불러오기 실패';
        });

    // ✅ 이번 주 다가오는 일정
    fetch(`/api/events/this-week-upcoming-count?workspaceCd=${workspaceCd}`)
        .then(res => res.json())
        .then(count => {
            document.getElementById('upcoming-this-week').innerText = `총 ${count}개 예정됨`;
        })
        .catch(err => {
            document.getElementById('upcoming-this-week').innerText = '불러오기 실패';
        });

    // ✅ 이번 주 생성된 일정
    fetch(`/api/events/this-week-created-count?workspaceCd=${workspaceCd}`)
        .then(res => res.json())
        .then(count => {
            document.getElementById('created-this-week').innerText = `총 ${count}개 생성됨`;
        })
        .catch(err => {
            document.getElementById('created-this-week').innerText = '불러오기 실패';
        });

    // ✅ 워크스페이스 참여 사용자 목록 로딩
    fetch(`/api/workspaces/${workspaceCd}/members`)
        .then(res => res.json())
        .then(data => {
            const members = data.members;
            const list = document.getElementById("workspace-members");
            list.innerHTML = "";

            if (!members || members.length === 0) {
                list.innerHTML = "<li>참여자가 없습니다.</li>";
                return;
            }

            members.forEach(member => {
                const li = document.createElement("li");
                li.textContent = member.userNickname || member.userId;
                li.style.cursor = "pointer";
                li.style.padding = "6px 0";

                // ✅ 모달 함수로 연결
                li.onclick = () => {
                    showUserDetailModal(member.userId);
                };

                list.appendChild(li);
            });
        })
        .catch(err => {
            console.error("❗ 워크스페이스 멤버 불러오기 실패:", err);
            document.getElementById("workspace-members").innerHTML = "<li>불러오기 실패</li>";
        });
});

function showUserDetailModal(userId) {
    const workspaceCd = localStorage.getItem("workspaceCd");

    fetch(`/api/workspaces/${workspaceCd}/member/${userId}`)
        .then(res => res.json())
        .then(user => {
            document.getElementById("detail-img").src = user.userImg || "/images/default-profile.png";
            document.getElementById("detail-name").textContent = user.userNickname || user.userId;
            document.getElementById("detail-email").textContent = user.email || "-";
            document.getElementById("detail-phone").textContent = user.phoneNum || "-";
            document.getElementById("detail-dept").textContent = user.deptNm || "-";
            document.getElementById("detail-position").textContent = user.position || "-";
            document.getElementById("detail-status").textContent = user.statusMsg || "-";

            document.getElementById("user-detail-modal").style.display = "block";
        })
        .catch(err => {
            alert("사용자 정보를 불러오지 못했습니다.");
            console.error(err);
        });
}

// 모달 닫기
document.addEventListener("DOMContentLoaded", function () {
    const closeBtn = document.querySelector(".close-button");
    if (closeBtn) {
        closeBtn.onclick = () => {
            document.getElementById("user-detail-modal").style.display = "none";
        };
    }
});

// ✅ hover 시 메뉴 보이기
document.addEventListener("DOMContentLoaded", function () {
    const profileContainer = document.querySelector(".mini-profile-container");
    const menu = document.querySelector(".mini-profile-menu");

    if (profileContainer && menu) {
        profileContainer.addEventListener("mouseenter", () => {
            menu.style.display = "block";
        });
        profileContainer.addEventListener("mouseleave", () => {
            menu.style.display = "none";
        });
    }

    // ✅ 사용자 정보 불러오기


    if (workspaceCd && userId) {
        fetch(`/api/workspaces/${workspaceCd}/member/${userId}`)
            .then(res => res.json())
            .then(user => {
                document.getElementById("mini-profile-img").src = user.userImg || "/images/default-profile.png";
                document.getElementById("mini-profile-name").textContent = user.userNickname || user.userId;
                document.getElementById("mini-profile-role").textContent = user.userRole || "MEMBER";
            });
    }
});

// ✅ 클릭 동작 정의
function goToMyPage() {
    const workspaceCd = localStorage.getItem("workspaceCd");
    const userId = localStorage.getItem("userId");

    fetch(`/api/workspaces/${workspaceCd}/member/${userId}`)
        .then(res => res.json())
        .then(user => {
            document.getElementById("my-img").src = user.userImg || "/images/default-profile.png";
            document.getElementById("my-name").textContent = user.userNickname || user.userId;
            document.getElementById("my-email").textContent = user.email || "-";
            document.getElementById("my-phone").textContent = user.phoneNum || "-";
            document.getElementById("my-dept").textContent = user.deptNm || "-";
            document.getElementById("my-position").textContent = user.position || "-";
            document.getElementById("my-status").textContent = user.statusMsg || "-";

            // 수정 모달에 값 미리 세팅
            document.getElementById("edit-email").value = user.email || "";
            document.getElementById("edit-nickname").value = user.userNickname || "";
            document.getElementById("edit-phone").value = user.phoneNum || "";
            document.getElementById("edit-dept").value = user.deptNm || "";
            document.getElementById("edit-position").value = user.position || "";
            document.getElementById("edit-status").value = user.statusMsg || "";

            document.getElementById("my-info-modal").style.display = "flex";
        })
        .catch(err => {
            alert("사용자 정보를 불러오지 못했습니다.");
            console.error(err);
        });
}

function openEditModal() {
    document.getElementById("my-info-modal").style.display = "none";
    document.getElementById("edit-info-modal").style.display = "flex";
    loadDepartmentOptions();
}

function submitEdit() {
    const workspaceCd = localStorage.getItem("workspaceCd");

    const formData = new FormData();
    formData.append("userNickname", document.getElementById("edit-nickname").value);
    formData.append("email", document.getElementById("edit-email").value);
    formData.append("phoneNum", document.getElementById("edit-phone").value);
    formData.append("deptCd", document.getElementById("edit-dept").value);
    formData.append("position", document.getElementById("edit-position").value);
    formData.append("statusMsg", document.getElementById("edit-status").value);

    const fileInput = document.getElementById("edit-img");
    if (fileInput.files.length > 0) {
        formData.append("userImg", fileInput.files[0]);
    }

    fetch(`/workspace/${workspaceCd}/set-profile`, {
        method: "POST",
        body: formData
    })
    .then(res => res.text())  // ✅ JSON 아님! plain text 처리
    .then(result => {
        const trimmed = result.trim();
        if (trimmed === "success") {
            alert("수정 완료!");
            document.getElementById("edit-info-modal").style.display = "none";
            goToMyPage();
        } else {
            alert("수정 실패: " + trimmed);
        }
    })
    .catch(err => {
        console.error("❌ 수정 실패:", err);
        alert("수정 중 오류 발생: " + err.message);
    });
}

function closeModal(id) {
    document.getElementById(id).style.display = "none";
}

function logout() {
    window.location.href = "/workspace";
}

function showStatus() {
    toggleStatusMenu();
}

// ✅ 상태 메뉴 열기/닫기
function toggleStatusMenu() {
    const modal = document.getElementById("status-modal");
    modal.style.display = modal.style.display === "block" ? "none" : "block";
}

// ✅ 상태 표시 업데이트
function updateStatusDisplay(status) {
    const display = document.querySelector(".user-status-display");
    if (!display) return;

    const statusMap = {
        online: "🟢 온라인",
        away: "🟡 자리비움",
        offline: "🔴 오프라인"
    };

    const label = statusMap[status.toLowerCase()] || "🟢 온라인";
    display.textContent = label;
    console.log("✅ 현재 사용자 상태:", status); // 디버깅용 출력
}

// ✅ 상태 불러오기
document.addEventListener("DOMContentLoaded", function () {
    const workspaceCd = localStorage.getItem("workspaceCd");
    const userId = localStorage.getItem("userId");

    if (!workspaceCd || !userId) {
        console.warn("⚠️ workspaceCd 또는 userId가 localStorage에 없습니다.");
        return;
    }

    fetch(`/api/workspaces/${workspaceCd}/member/${userId}/status`)
        .then(res => {
            if (!res.ok) throw new Error("서버 응답 오류");
            return res.text();
        })
        .then(status => {
            updateStatusDisplay(status);
        })
        .catch(err => {
            console.error("❌ 상태 불러오기 실패:", err);
            updateStatusDisplay("online");
        });
});

function changeStatus(newStatus) {
    const workspaceCd = localStorage.getItem("workspaceCd");
    const userId = localStorage.getItem("userId");

    fetch(`/api/workspaces/${workspaceCd}/member/${userId}/status`, {
        method: "PATCH",
        headers: { "Content-Type": "text/plain" },
        body: newStatus
    })
    .then(res => {
        if (!res.ok) throw new Error("업데이트 실패");
        return res.text();
    })
    .then(msg => {
        console.log("✅ 상태 변경 성공:", msg);
        updateStatusDisplay(newStatus); // UI 갱신
        document.getElementById("status-modal").style.display = "none"; // 모달 닫기
    })
    .catch(err => {
        console.error("❌ 상태 업데이트 실패:", err);
        alert("상태 변경 중 오류 발생");
    });
}

// 부서 불러오기
function loadDepartmentOptions() {
    const workspaceCd = localStorage.getItem("workspaceCd");
    const select = document.getElementById("edit-dept");

    fetch(`/api/workspaces/${workspaceCd}/departments`)
        .then(res => res.json())
        .then(depts => {
            select.innerHTML = ""; // 초기화
            depts.forEach(dept => {
                const option = document.createElement("option");
                option.value = dept.deptCd;
                option.textContent = dept.deptNm;
                select.appendChild(option);
            });
        })
        .catch(err => {
            console.error("부서 목록 불러오기 실패", err);
        });
}

// ✅ 모든 close-button 에 이벤트 연결
document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll(".close-button").forEach(btn => {
        btn.addEventListener("click", function () {
            this.closest(".modal").style.display = "none";
        });
    });
});

function generateNotificationMessage(noti) {
    const date = new Date(noti.createdDate);
    const time = date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });

    switch (noti.notiState) {
        case "NEW_ATTENDENCE":
            return `${noti.createdBy}님이 워크스페이스에 참여하였습니다 (${time})`;
        case "NEW_EVENT":
            return `${noti.createdBy}님이 새로운 일정을 등록하였습니다 (${time})`;
        case "NEW_MEETING":
            return `${noti.createdBy}님이 새로운 미팅을 등록하였습니다 (${time})`;
        default:
            return `${noti.createdBy}님의 활동이 감지되었습니다 (${time})`;
    }
}

function respondToInvite(userId, action) {
    const workspaceCd = localStorage.getItem("workspaceCd");

    fetch(`/api/workspaces/${workspaceCd}/invitations/respond`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ invitedUserId: userId, status: action })
    })
    .then(res => {
        console.log("✅ 응답 상태:", res.status);
        return res.text();
    })
    .then(msg => {
        alert("서버 응답:\n" + msg);
        location.reload();  // 새로고침으로 리스트 갱신
    })
    .catch(err => {
        alert("에러 발생: " + err.message);
    });
}
