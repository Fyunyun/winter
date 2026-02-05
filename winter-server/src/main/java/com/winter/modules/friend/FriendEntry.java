package com.winter.modules.friend;

public class FriendEntry {
    private final long friendId;
        private final int status; // 0:申请中, 1:好友

        public FriendEntry(long friendId, int status) {
            this.friendId = friendId;
            this.status = status;
        }

        public long getFriendId() {
            return friendId;
        }

        public int getStatus() {
            return status;
        }

}
