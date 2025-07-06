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
    fetch(`/api/events/today?userId=${userId}`)
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
});
