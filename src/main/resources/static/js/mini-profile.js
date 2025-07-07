document.addEventListener("DOMContentLoaded", async () => {
  const profileBtn = document.querySelector(".user-box");
  const modal = document.getElementById("miniProfileModal");

  const workspaceCd = new URLSearchParams(window.location.search).get("workspaceCd");
  if (!workspaceCd || !profileBtn || !modal) return;

  const statusMap = {
    "ONLINE": { icon: "green_circle.png", label: "온라인" },
    "AWAY": { icon: "red_circle.png", label: "자리비움" },
    "OFFLINE": { icon: "gray_circle.png", label: "오프라인" }
  };

  profileBtn.addEventListener("click", async () => {
    try {
      const res = await fetch(`/api/workspaces/${workspaceCd}/profile`);
      const profile = await res.json();

      document.getElementById("miniProfileImg").src = profile.userImg || "/images/default.png";
      document.getElementById("miniProfileName").textContent = profile.userNickname || "";
      document.getElementById("miniProfileRole").textContent = profile.position || "";

      const status = (profile.status || "OFFLINE").toUpperCase();
      setStatus(status);

      modal.style.left = "60px";
      modal.style.opacity = "1";
    } catch (err) {
      console.error("Mini profile fetch error", err);
    }
  });

  window.addEventListener("click", (e) => {
    if (!modal.contains(e.target) && !profileBtn.contains(e.target)) {
      modal.style.left = "-300px";
      modal.style.opacity = "0";
      document.getElementById("statusOptions").style.display = "none";
    }
  });

  window.toggleStatusMenu = function () {
    const box = document.getElementById("statusOptions");
    box.style.display = box.style.display === "block" ? "none" : "block";
  };

  window.setStatus = function (status) {
    const statusInfo = statusMap[status] || statusMap["OFFLINE"];
    document.getElementById("miniProfileStatus").src = "/images/" + statusInfo.icon;
    document.getElementById("currentStatusLabel").textContent = statusInfo.label;
    document.getElementById("statusOptions").style.display = "none";
  };
});
