package dev.soon.interviewdefense.defense.controller;

import dev.soon.interviewdefense.chat.controller.dto.ChatMessageDto;
import dev.soon.interviewdefense.chat.controller.dto.DefenseChatRoomReqDto;
import dev.soon.interviewdefense.chat.domain.Chat;
import dev.soon.interviewdefense.chat.domain.ChatMessage;
import dev.soon.interviewdefense.chat.service.ChatService;
import dev.soon.interviewdefense.security.SecurityUser;
import dev.soon.interviewdefense.user.domain.Language;
import dev.soon.interviewdefense.user.domain.Tech;
import dev.soon.interviewdefense.user.domain.User;
import dev.soon.interviewdefense.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class DefenseController {

    private final ChatService chatService;
    private final UserService userService;

    @GetMapping("/defense/create")
    public String defenseChatRoom(@AuthenticationPrincipal SecurityUser securityUser,
                                  Model model) {
        User loginUserInfo = userService.getLoginUserInfo(securityUser);
        List<Language> loginUserLanguages = userService.getLoginUserLanguages(securityUser);
        List<Tech> loginUserTechs = userService.getLoginUserTechs(securityUser);
        model.addAttribute("dto", new DefenseChatRoomReqDto(null, null));
        model.addAttribute("user", loginUserInfo);
        model.addAttribute("myLanguages", loginUserLanguages);
        model.addAttribute("myTechs", loginUserTechs);
        return "defenseChatRoomForm";
    }

    @PostMapping("/defense/create")
    public String createDefenseChatRoom(@AuthenticationPrincipal SecurityUser securityUser,
                                        @ModelAttribute("dto") DefenseChatRoomReqDto dto) {
        Long chatRoomId = chatService.createDefenseChatRoom(securityUser, dto);
        String res = chatService.initDefensePrompt(securityUser, dto);
        chatService.saveAIMessage(chatRoomId, securityUser, res);
        return "redirect:/chat/defense/" + chatRoomId;
    }

    @GetMapping("/defense/{chatRoomId}")
    public String getDefenseChatRoom(@AuthenticationPrincipal SecurityUser securityUser,
                                     @PathVariable Long chatRoomId, Model model) {
        Chat chatRoom = chatService.getChatRoom(securityUser, chatRoomId);
        List<ChatMessage> chatMessagesInChatRoom = chatService.getChatRoomMessages(chatRoom);
        model.addAttribute("chatMessages", chatMessagesInChatRoom);
        model.addAttribute("chat", chatRoom);
        User loginUserInfo = userService.getLoginUserInfo(securityUser);
        model.addAttribute("user", loginUserInfo);
        model.addAttribute("chatMessageDto", new ChatMessageDto(null));
        return "defenseChatRoom";
    }

    @PostMapping("/defense/{chatRoomId}")
    public String sendMessageToDefense(@PathVariable Long chatRoomId,
                                       @AuthenticationPrincipal SecurityUser securityUser,
                                       @ModelAttribute("chatMessageDto") ChatMessageDto dto) {
        Chat chat = chatService.saveUserMessage(chatRoomId, securityUser, dto);
        String response = chatService.generateDefensePrompt(chat, securityUser, dto);
        chatService.saveAIMessage(chatRoomId, securityUser, response);
        return "redirect:/chat/defense/{chatRoomId}";
    }
}