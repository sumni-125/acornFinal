function copyInviteCode() {
            const code = document.getElementById("inviteCode").innerText;
            navigator.clipboard.writeText(code).then(() => {
                alert("초대코드가 복사되었습니다: " + code);
            }).catch(err => {
                alert("복사 실패: " + err);
            });
        }

        function openInviteModal() {
            document.getElementById("inviteModal").style.display = "flex";
            // modal에 초대코드 자동 세팅
            const code = document.getElementById("inviteCode")?.innerText;
            document.getElementById("modalInviteCode").value = code || "";
        }

        function closeInviteModal() {
            document.getElementById("inviteModal").style.display = "none";
        }

        function copyInviteCodeFromModal() {
            const code = document.getElementById("modalInviteCode").value;
            navigator.clipboard.writeText(code).then(() => {
                alert("초대코드가 복사되었습니다: " + code);
            }).catch(err => {
                alert("복사 실패: " + err);
            });
        }

        function sendInviteEmail() {
            const email = document.getElementById("inviteEmail").value;
            const code = document.getElementById("modalInviteCode").value;

            if (!email || !code) {
                alert("이메일과 초대코드를 모두 확인해주세요.");
                return;
            }

            fetch('/api/workspaces/invite-email', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email: email, inviteCode: code })
            })
            .then(res => {
                if (res.ok) return res.text();
                throw new Error("이메일 전송 실패");
            })
            .then(msg => {
                alert("이메일이 전송되었습니다.");
                closeInviteModal();
            })
            .catch(err => {
                alert("전송 실패: " + err.message);
            });
        }
