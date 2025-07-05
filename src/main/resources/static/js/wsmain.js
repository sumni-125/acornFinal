document.addEventListener("DOMContentLoaded", function () {
    const userId = localStorage.getItem("userId");
    const workspaceCd = localStorage.getItem("workspaceCd");

    console.log("📦 로딩 시작 - userId:", userId, ", workspaceCd:", workspaceCd);

    if (!userId || !workspaceCd) {
        console.warn("⚠️ userId 또는 workspaceCd가 localStorage에 없습니다.");
        return;
    }

    // ✅ 오늘 일정 불러오기
    console.log("📡 오늘 일정 요청 시작");
    fetch(`/api/events/today?userId=${userId}`)
        .then(response => {
            console.log("📬 오늘 일정 응답 상태:", response.status);
            if (!response.ok) throw new Error("❌ 오늘 일정 조회 실패");
            return response.json();
        })
        .then(data => {
            console.log("📥 오늘 일정 데이터 수신:", data);
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

    // ✅ 이번 주 완료 일정 (워크스페이스 전체 대상)
    console.log("📡 이번 주 완료 일정 요청 시작");
    fetch(`/api/events/this-week-completed-count?workspaceCd=${workspaceCd}`)
        .then(res => {
            console.log("📬 이번 주 완료 응답 상태:", res.status);
            return res.json();
        })
        .then(count => {
            console.log("📥 완료 일정 개수 수신:", count);
            document.getElementById('completed-this-week').innerText = `총 ${count}개 완료됨`;
        })
        .catch(err => {
            console.error("❗ 이번 주 완료 일정 오류:", err);
            document.getElementById('completed-this-week').innerText = '불러오기 실패';
        });

        // ✅ 이번 주 다가오는 일정
        console.log("📡 이번 주 다가오는 일정 요청 시작");
        fetch(`/api/events/this-week-upcoming-count?workspaceCd=${workspaceCd}`)
            .then(res => {
                console.log("📬 응답 상태:", res.status);
                return res.json();
            })
            .then(count => {
                console.log("📥 다가오는 일정 수:", count);
                document.getElementById('upcoming-this-week').innerText = `총 ${count}개 예정됨`;
            })
            .catch(err => {
                console.error("❗ 다가오는 일정 오류:", err);
                document.getElementById('upcoming-this-week').innerText = '불러오기 실패';
            });

        // ✅ 이번 주 새로 생성된 일정
        console.log("📡 이번 주 생성 일정 요청 시작");
        fetch(`/api/events/this-week-created-count?workspaceCd=${workspaceCd}`)
            .then(res => {
                console.log("📬 응답 상태:", res.status);
                return res.json();
            })
            .then(count => {
                console.log("📥 생성 일정 수:", count);
                document.getElementById('created-this-week').innerText = `총 ${count}개 생성됨`;
            })
            .catch(err => {
                console.error("❗ 생성 일정 불러오기 실패:", err);
                document.getElementById('created-this-week').innerText = '불러오기 실패';
            });

        document.addEventListener("DOMContentLoaded", function() {
            const workspaceCd = getWorkspaceCdFromURL(); // URL 파라미터에서 추출 등 방식
            fetch(`/workspace/${workspaceCd}/usage-time`)
              .then(res => res.json())
              .then(seconds => {
                  const hours = Math.floor(seconds / 3600);
                  const minutes = Math.floor((seconds % 3600) / 60);
                  document.getElementById("usage-time").textContent = `${hours}시간 ${minutes}분`;
              })
              .catch(err => {
                  console.error("사용 시간 로드 실패:", err);
                  document.getElementById("usage-time").textContent = "불러오기 실패";
              });
        });

});
