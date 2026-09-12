package com.forum.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.forum.api.dto.user.UpdateUserSettingsRequest;

import tools.jackson.databind.json.JsonMapper;

class UpdateUserSettingsRequestTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void absentFieldIsNotProvided() {
        UpdateUserSettingsRequest request = mapper.readValue("{}", UpdateUserSettingsRequest.class);

        assertThat(request.isProvided()).isFalse();
        assertThat(request.getMaxReplyDepth()).isNull();
    }

    @Test
    void explicitNullMeansUnlimited() {
        UpdateUserSettingsRequest request = mapper.readValue("{\"maxReplyDepth\":null}",
                UpdateUserSettingsRequest.class);

        assertThat(request.isProvided()).isTrue();
        assertThat(request.isUnlimited()).isTrue();
        assertThat(request.getMaxReplyDepth()).isEmpty();
    }

    @Test
    void valueIsProvided() {
        UpdateUserSettingsRequest request = mapper.readValue("{\"maxReplyDepth\":5}",
                UpdateUserSettingsRequest.class);

        assertThat(request.isProvided()).isTrue();
        assertThat(request.isUnlimited()).isFalse();
        assertThat(request.getMaxReplyDepth()).contains(5);
    }
}
