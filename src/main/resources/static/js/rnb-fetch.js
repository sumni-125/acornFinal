document.addEventListener("DOMContentLoaded", async () => {
  const rnbContainer = document.getElementById("rnbContainer");

  try {
    const workspaceCd = rnbContainer?.dataset.workspaceCd;
    if (!workspaceCd) {
      console.error("workspaceCd 없음");
      return;
    }

    // 🔹 RNB HTML 삽입
    const rnbHtml = await fetch("/html/rnb.html").then(res => {
      if (!res.ok) throw new Error("rnb.html 불러오기 실패");
      return res.text();
    });
    rnbContainer.innerHTML = rnbHtml;

    await new Promise(resolve => setTimeout(resolve, 10));

    // 🔹 RNB 토글 기능
    const btnImg = document.querySelector('.rnb-toggle-btn img');
    const body = document.body;
    if (btnImg) {
      btnImg.style.transform = 'rotate(0deg)';
      document.querySelector('.rnb-toggle-btn').addEventListener('click', () => {
        const isClosed = body.classList.contains('rnb-closed');
        body.classList.toggle('rnb-closed');
        btnImg.style.transform = isClosed ? 'rotate(0deg)' : 'rotate(180deg)';
      });
    }

    // 🔹 이미지 경로 처리
    const getImagePath = (img) => {
      if (!img) return "/images/default.png";
      if (img.startsWith("/") || img.startsWith("http")) return img;
      return `/images/${img}`;
    };

    // 🔹 내 프로필
    const profileRes = await fetch(`/api/workspaces/${workspaceCd}/profile`);
    if (!profileRes.ok) throw new Error("프로필 API 실패");
    const myProfile = await profileRes.json();

    document.getElementById("myProfileImg").src = getImagePath(myProfile.userImg);
    document.getElementById("myProfileName").textContent = myProfile.userNickname || "이름없음";
    document.getElementById("myProfileRole").textContent = myProfile.position || "직급없음";
    document.getElementById("myProgressBar").style.width = (myProfile.progress || 0) + "%";
    document.getElementById("myProgressPercent").textContent = (myProfile.progress || 0) + "%";

    // 🔹 멤버 목록
    const memberRes = await fetch(`/api/workspaces/${workspaceCd}/members`);
    if (!memberRes.ok) throw new Error("멤버 API 실패");
    const data = await memberRes.json();
    const members = data.members || [];

    document.getElementById("memberCount").textContent = members.length;
    const memberContainer = document.getElementById("memberListContainer");

    members.forEach(member => {
      const memberDiv = document.createElement("div");
      memberDiv.classList.add("member");
      memberDiv.innerHTML = `
        <img src="${getImagePath(member.userImg)}" alt="멤버이미지">
        <div class="info">
          <span class="m-name">${member.userNickname}</span>
          <span class="m-role">${member.position}</span>
        </div>
      `;
      memberContainer.appendChild(memberDiv);
    });

    // 🔹 초대 모달 삽입
    const modalRes = await fetch("/html/invite-modal.html");
    if (!modalRes.ok) throw new Error("invite-modal.html 로딩 실패");
    const modalHtml = await modalRes.text();
    document.body.insertAdjacentHTML("beforeend", modalHtml);

    await new Promise(resolve => setTimeout(resolve, 10));

    // 🔹 워크스페이스 정보 가져오기 (inviteCode, workspaceName)
    const infoRes = await fetch(`/api/workspaces/${workspaceCd}/info`);
    if (!infoRes.ok) throw new Error("워크스페이스 정보 API 실패");
    const workspaceInfo = await infoRes.json();

    // 🔹 모달 요소 바인딩
    const inviteBtn = document.querySelector(".invite-member");
    const modal = document.getElementById("inviteModal");
    const overlay = document.getElementById("inviteOverlay");
    const closeBtn = modal?.querySelector(".close-btn");

    const emailInput = document.getElementById("inviteEmail");
    const emailError = document.getElementById("emailError");
    const emailSuccess = document.getElementById("emailSuccess");
    const copySuccess = document.getElementById("copySuccess");
    const sendBtn = document.querySelector(".send-btn");
    const copyBtn = document.querySelector(".copy-btn");
    const inviteCode = document.getElementById("inviteCode");
    const workspaceNameHeader = document.getElementById("workspaceNameHeader");

    // 🔹 실제 워크스페이스 이름과 코드 표시
    workspaceNameHeader.textContent = `${workspaceInfo.workspaceName}(으)로 초대하기`;
    inviteCode.textContent = workspaceInfo.inviteCode;

    // 🔹 모달 열기
    inviteBtn?.addEventListener("click", (e) => {
      e.stopPropagation();
      modal.style.display = "block";
      overlay.style.display = "block";
      emailInput.value = "";
      emailError.style.display = "none";
      emailSuccess.style.display = "none";
      copySuccess.style.display = "none";
    });

    // 🔹 모달 닫기
    const closeModal = () => {
      modal.style.display = "none";
      overlay.style.display = "none";
    };
    closeBtn?.addEventListener("click", closeModal);
    overlay?.addEventListener("click", (e) => {
      if (e.target === overlay) closeModal();
    });

    modal?.addEventListener("click", (e) => {
      e.stopPropagation();
    });

    // 🔹 이메일 전송
    sendBtn?.addEventListener("click", () => {
      const email = emailInput.value.trim();
      const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

      emailError.style.display = "none";
      emailSuccess.style.display = "none";

      if (!regex.test(email)) {
        emailError.style.display = "block";
        return;
      }

      fetch("/api/workspaces/invite-email", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email, inviteCode: workspaceInfo.inviteCode })
      })
        .then(res => {
          if (!res.ok) throw new Error("전송 실패");
          return res.text();
        })
        .then(() => {
          emailSuccess.style.display = "block";
        })
        .catch(err => {
          emailError.textContent = "전송 실패: " + err.message;
          emailError.style.display = "block";
        });
    });

    // 🔹 초대 코드 복사
    copyBtn?.addEventListener("click", () => {
      navigator.clipboard.writeText(workspaceInfo.inviteCode)
        .then(() => {
          copySuccess.style.display = "block";
        })
        .catch(() => alert("복사 실패"));
    });

  } catch (err) {
    console.error("🔴 RNB 전체 로딩 중 에러:", err);
  }
});
