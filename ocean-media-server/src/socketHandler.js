// src/socketHandler.js
const roomManager = require('./RoomManager');
const Peer = require('./Peer');

const peers = new Map();

module.exports = (io, worker, router) => {
  io.on('connection', (socket) => {
    console.log('New client connected:', socket.id);

    socket.on('join-room', async (data) => {
      try {
        const { roomId, workspaceId, peerId, displayName } = data;
        console.log(`${displayName} joining room ${roomId}`);

        // 룸 가져오기 또는 생성
        let room = roomManager.getRoom(roomId);
        if (!room) {
          room = roomManager.createRoom(roomId, workspaceId, router);
        }

        // Peer 생성
        const peer = new Peer(socket, roomId, peerId, displayName);
        peers.set(socket.id, peer);

        // 룸에 피어 추가
        room.addPeer(peerId, { displayName, socketId: socket.id });

        // Socket.io 룸 참가
        socket.join(roomId);

        // 기존 참가자 정보 전송
        const existingPeers = room.getAllPeers()
          .filter(p => p.id !== peerId)
          .map(p => p.toJson());

        socket.emit('room-joined', {
          roomId,
          peers: existingPeers
        });

        // 다른 참가자들에게 알림
        socket.to(roomId).emit('new-peer', {
          peerId,
          displayName
        });

      } catch (error) {
        console.error('Join room error:', error);
        socket.emit('error', { message: error.message });
      }
    });

    // Router RTP Capabilities 요청
    socket.on('get-router-rtp-capabilities', (callback) => {
      callback(router.rtpCapabilities);
    });

    // Transport 생성
    socket.on('create-transport', async (data, callback) => {
      try {
        const { producing, consuming } = data;
        const peer = peers.get(socket.id);

        if (!peer) {
          throw new Error('Peer not found');
        }

        const transport = await createWebRtcTransport(router);
        peer.addTransport(transport);

        callback({
          id: transport.id,
          iceParameters: transport.iceParameters,
          iceCandidates: transport.iceCandidates,
          dtlsParameters: transport.dtlsParameters
        });

      } catch (error) {
        console.error('Create transport error:', error);
        callback({ error: error.message });
      }
    });

    // 연결 해제
    socket.on('disconnect', () => {
      const peer = peers.get(socket.id);
      if (!peer) return;

      console.log(`Client disconnected: ${peer.displayName}`);

      // 룸에서 피어 제거
      const room = roomManager.getRoom(peer.roomId);
      if (room) {
        room.removePeer(peer.id);

        // 다른 참가자들에게 알림
        socket.to(peer.roomId).emit('peer-left', { peerId: peer.id });

        // 빈 룸 삭제
        if (room.isEmpty()) {
          roomManager.deleteRoom(peer.roomId);
        }
      }

      // 피어 정리
      peer.close();
      peers.delete(socket.id);
    });
  });
};

// WebRTC Transport 생성 함수
async function createWebRtcTransport(router) {
  const transport = await router.createWebRtcTransport({
    listenIps: [
      {
        ip: process.env.MEDIASOUP_LISTEN_IP || '127.0.0.1',
        announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || '127.0.0.1'
      }
    ],
    enableUdp: true,
    enableTcp: true,
    preferUdp: true,
    initialAvailableOutgoingBitrate: 1000000
  });

  return transport;
}