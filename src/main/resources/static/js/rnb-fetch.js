document.addEventListener("DOMContentLoaded", async () => {
    const rnbContainer = document.getElementById("rnbContainer");

    try {
        const urlParams = new URLSearchParams(window.location.search);
        const workspaceCd = urlParams.get("workspaceCd");
        if (!workspaceCd) {
            console.error("workspaceCd 없음");
            return;
        }

        // RNB HTML을 가져와서 삽입
        const rnbHtml = await fetch("/fragments/rnb.html")
            .then(res => res.text());
        rnbContainer.innerHTML = rnbHtml;

        // 1. 프로필 정보
        const profileRes = await fetch(`/api/workspaces/${workspaceCd}/profile`);
        const myProfile = await profileRes.json();

        document.getElementById("myProfileImg").src = "/profile-images/" + (myProfile.image || "default.png");
        document.getElementById("myProfileName").textContent = myProfile.userNickname || "이름없음";
        document.getElementById("myProfileRole").textContent = myProfile.position || "회원";
        document.getElementById("myProgressBar").style.width = myProfile.progress + "%";
        document.getElementById("myProgressPercent").textContent = myProfile.progress + "%";

        // 2. 멤버 리스트
        const memberRes = await fetch(`/api/workspaces/${workspaceCd}`);
        const data = await memberRes.json();
        const members = data.members || [];

        const memberContainer = document.getElementById("memberListContainer");
        const memberCountEl = document.getElementById("memberCount");
        memberCountEl.textContent = members.length;

        members.forEach(member => {
            const memberDiv = document.createElement("div");
            memberDiv.classList.add("member");

            memberDiv.innerHTML = `
                <img src="/profile-images/${member.image || 'default.png'}" alt="멤버이미지">
                <div class="info">
                    <span class="m-name">${member.userNickname}</span>
                    <span class="m-role">${member.position}</span>
                </div>
            `;
            memberContainer.appendChild(memberDiv);
        });

    } catch (err) {
        console.error("RNB 불러오는 중 에러:", err);
    }
});
