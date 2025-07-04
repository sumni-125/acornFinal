document.addEventListener("DOMContentLoaded", () => {
    const btnImg = document.querySelector('.rnb-toggle-btn img');
    const body = document.body;

    // 초기 방향 설정
    btnImg.style.transform = 'rotate(0deg)';

    // 사이드바 토글
    document.querySelector('.rnb-toggle-btn').addEventListener('click', () => {
        const isClosed = body.classList.contains('rnb-closed');
        body.classList.toggle('rnb-closed');
        btnImg.style.transform = isClosed ? 'rotate(0deg)' : 'rotate(180deg)';
    });
});



