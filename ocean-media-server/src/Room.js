class Room {
  constructor(roomId, workspaceId, router) {
    this.id = roomId;
    this.workspaceId = workspaceId;
    this.router = router;
    this.peers = new Map();
    this.createdAt = new Date();
  }

  addPeer(peerId, peerInfo) {
    const peer = {
      id: peerId,
      ...peerInfo,
      transports: new Map(),
      producers: new Map(),
      consumers: new Map(),
      joinedAt: new Date()
    };

    this.peers.set(peerId, peer);
    console.log(`Peer ${peerId} joined room ${this.id}`);
    return peer;
  }

  removePeer(peerId) {
    const peer = this.peers.get(peerId);
    if (!peer) return;

    // Transport 정리
    peer.transports.forEach(transport => transport.close());

    // Producer 정리
    peer.producers.forEach(producer => producer.close());

    // Consumer 정리
    peer.consumers.forEach(consumer => consumer.close());

    this.peers.delete(peerId);
    console.log(`Peer ${peerId} left room ${this.id}`);
  }

  getPeer(peerId) {
    return this.peers.get(peerId);
  }

  getAllPeers() {
    return Array.from(this.peers.values());
  }

  isEmpty() {
    return this.peers.size === 0;
  }

  toJson() {
    return {
      id: this.id,
      workspaceId: this.workspaceId,
      peers: this.getAllPeers().map(peer => ({
        id: peer.id,
        displayName: peer.displayName,
        joinedAt: peer.joinedAt
      })),
      createdAt: this.createdAt
    };
  }
}

module.exports = Room;

