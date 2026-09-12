package com.forum.api.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forum.api.dto.discussion.DiscussionSummary;
import com.forum.api.dto.user.SettingsResponse;
import com.forum.api.dto.user.UpdateUserSettingsRequest;
import com.forum.api.dto.user.UserResponse;
import com.forum.api.model.User;
import com.forum.api.service.DiscussionService;
import com.forum.api.service.UserService;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;
    private final DiscussionService discussionService;

    public UserController(UserService userService, DiscussionService discussionService) {
        this.userService = userService;
        this.discussionService = discussionService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal User user) {
        return userService.getById(user.id());
    }

    @GetMapping("/settings")
    public SettingsResponse settings(@AuthenticationPrincipal User user) {
        return userService.getSettings(user.id());
    }

    @PatchMapping("/settings")
    public SettingsResponse updateSettings(@AuthenticationPrincipal User user,
            @RequestBody UpdateUserSettingsRequest request) {
        return userService.updateSettings(user.id(), request);
    }

    @GetMapping("/discussions")
    public List<DiscussionSummary> myDiscussions(@AuthenticationPrincipal User user) {
        return discussionService.findByAuthorId(user.id());
    }
}
