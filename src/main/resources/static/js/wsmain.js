document.addEventListener("DOMContentLoaded", function () {
    const userId = localStorage.getItem("userId");
    const workspaceCd = localStorage.getItem("workspaceCd");
    if (!userId || !workspaceCd) return;

    // ✅ 오늘 일정 불러오기
    fetch(`/api/events/today?userId=${userId}`)
        .then(response => {
            if (!response.ok) throw new Error("일정 조회 실패");
            return response.json();
        })
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
            console.error(error);
            document.getElementById("user-events-list").innerHTML = "<li>일정 정보를 불러올 수 없습니다.</li>";
        });

    // ✅ 이번 주 완료 일정 (워크스페이스 전체 대상)
    fetch(`/api/events/this-week-completed-count?workspaceCd=${workspaceCd}`)
        .then(res => res.json())
        .then(count => {
            document.getElementById('completed-this-week').innerText = `총 ${count}개 완료됨`;
        })
        .catch(err => {
            console.error('이번주 완료 일정 불러오기 실패:', err);
            document.getElementById('completed-this-week').innerText = '불러오기 실패';
        });
});
