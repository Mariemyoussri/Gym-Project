package com.example.gymserver.controllers;

import com.example.gymserver.dto.ProgramFollowUpDTO;
import com.example.gymserver.dto.UserDTO;
import com.example.gymserver.dto.UserIdDTO;
import com.example.gymserver.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     *
     * @param userName
     * @param userIdDTO
     * @return user info if userId is matched to the username
     *            null otherwise
     */
    @PostMapping("/profile/{userName}")
    public UserDTO getUserProfile(@PathVariable("userName") String userName, @RequestBody UserIdDTO userIdDTO){
        return this.userService.getUserProfile(userIdDTO.getUserId(), userName);
    }

    /**
     *
     * @param user
     */
    @PostMapping("/update-profile/{userName}")
    public int updateUserInfo(@RequestBody UserDTO user){
        return userService.updateUserInfo(user);
    }






}
