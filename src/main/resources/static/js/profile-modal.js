// profile-modal.js

document.addEventListener('DOMContentLoaded', function () {
  const workspaceCd = new URLSearchParams(location.search).get("workspaceCd");
  const editBtn = document.getElementById('editProfileBtn');
  const closeBtn = document.getElementById('closeProfileModal');
  const saveBtn = document.getElementById('saveProfileBtn');
  const selectedDept = document.getElementById('selectedDept');
  const deptList = document.getElementById('deptList');

  // 열기 함수 (외부에서 호출)
  window.openProfileModal = async function () {
    const res = await fetch(`/api/workspaces/${workspaceCd}/profile`);
    const profile = await res.json();

    document.getElementById('viewName').value = profile.userNickname || '';
    document.getElementById('viewPhone').value = profile.phoneNum || '';
    document.getElementById('viewEmail').value = profile.email || '';
    document.getElementById('viewPosition').value = profile.position || '';
    document.getElementById('viewStatusMsg').value = profile.statusMsg || '';
    document.getElementById('selectedDept').dataset.deptcd = profile.deptCd || '';
    document.getElementById('selectedDept').querySelector('span').textContent = profile.deptNm || '부서를 선택하세요';

    // 소셜 로그인 표시
    const socialInfo = document.getElementById('socialInfo');
    socialInfo.innerHTML = profile.platform === 'kakao'
      ? '<img src="/images/kakao_logo.png"> Kakao'
      : '<img src="/images/google_logo.png"> Google';

    document.getElementById('profileModalOverlay').style.display = 'block';
    document.getElementById('profileModal').style.display = 'block';
  };

  closeBtn.addEventListener('click', () => {
    document.getElementById('profileModalOverlay').style.display = 'none';
    document.getElementById('profileModal').style.display = 'none';
  });

  editBtn.addEventListener('click', () => {
    document.querySelectorAll('.form-group input').forEach(el => el.removeAttribute('readonly'));
    saveBtn.style.display = 'block';
  });

  selectedDept.addEventListener('click', () => {
    deptList.style.display = deptList.style.display === 'block' ? 'none' : 'block';
  });

  deptList.addEventListener('click', (e) => {
    if (e.target.tagName === 'LI') {
      const cd = e.target.dataset.deptcd;
      const name = e.target.textContent;
      selectedDept.dataset.deptcd = cd;
      selectedDept.querySelector('span').textContent = name;
      deptList.style.display = 'none';
    }
  });

  saveBtn.addEventListener('click', async () => {
    const payload = {
      userNickname: document.getElementById('viewName').value,
      phoneNum: document.getElementById('viewPhone').value,
      email: document.getElementById('viewEmail').value,
      position: document.getElementById('viewPosition').value,
      statusMsg: document.getElementById('viewStatusMsg').value,
      deptCd: selectedDept.dataset.deptcd,
      userRole: 'MEMBER'
    };

    const res = await fetch(`/api/workspaces/${workspaceCd}/profile`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      alert("저장이 완료되었습니다.");
      location.reload();
    } else {
      alert("저장에 실패했습니다.");
    }
  });

  // 부서 목록 조회
  async function fetchDepts() {
    const res = await fetch(`/api/workspaces/${workspaceCd}/departments`);
    const depts = await res.json();
    deptList.innerHTML = '';
    depts.forEach(dept => {
      const li = document.createElement('li');
      li.dataset.deptcd = dept.deptCd;
      li.textContent = dept.deptNm;
      deptList.appendChild(li);
    });
  }

  fetchDepts();
});
