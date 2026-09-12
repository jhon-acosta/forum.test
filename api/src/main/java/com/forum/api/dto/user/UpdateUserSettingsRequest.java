package com.forum.api.dto.user;

import java.util.Optional;

public class UpdateUserSettingsRequest {

    private Optional<Integer> maxReplyDepth;

    public Optional<Integer> getMaxReplyDepth() {
        return maxReplyDepth;
    }

    public void setMaxReplyDepth(Optional<Integer> maxReplyDepth) {
        this.maxReplyDepth = maxReplyDepth;
    }

    public boolean isProvided() {
        return maxReplyDepth != null;
    }

    public boolean isUnlimited() {
        return maxReplyDepth != null && maxReplyDepth.isEmpty();
    }
}
