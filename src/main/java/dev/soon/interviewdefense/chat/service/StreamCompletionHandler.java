package dev.soon.interviewdefense.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import dev.soon.interviewdefense.chat.domain.Chat;
import dev.soon.interviewdefense.chat.domain.ChatMessage;
import dev.soon.interviewdefense.chat.domain.ChatSender;
import dev.soon.interviewdefense.chat.event.MessageSendEvent;
import dev.soon.interviewdefense.chat.respository.ChatCacheStore;
import dev.soon.interviewdefense.chat.respository.ChatRepository;
import dev.soon.interviewdefense.chat.util.PromptGenerator;
import dev.soon.interviewdefense.exception.ApiException;
import dev.soon.interviewdefense.exception.CustomErrorCode;
import dev.soon.interviewdefense.open_ai.service.OpenAiChatService;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamCompletionHandler extends TextWebSocketHandler {

    private final ChatCacheStore cacheStore;
    private final ChatRepository chatRepository;
    private final OpenAiChatService openAiChatService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public final static String DEEP_QUESTION_FLAG = "%deepQ%";
    private final static String DEEP_DIVE = "DEEP DIVE!";
    private final static String START_CHAT_FLAG = "%start%";
    private static long start;

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String chatSessionId = session.getId();
        Objects.requireNonNull(chatSessionId);
        cacheStore.removeCache(chatSessionId);
        session.close();
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        start = System.currentTimeMillis();
        String payload = message.getPayload();
        String chatSessionId = session.getId();
        if(hasStartFlag(payload)) {
            saveChatInMap(payload, chatSessionId);
            return;
        }
        StringBuilder sb = new StringBuilder();
        Chat chat = cacheStore.getChatByCacheKey(chatSessionId);
        if (hasDeepFlag(payload)) {
            applicationEventPublisher.publishEvent(new MessageSendEvent(new ChatMessage(DEEP_DIVE, chat, ChatSender.USER)));
            Flowable<ChatCompletionChunk> responseFlowable =
                    openAiChatService.generateStreamResponse(chat, "[" + payload.replace(DEEP_QUESTION_FLAG, "").trim() + "]" + PromptGenerator.DEEP_DIVE);
            subscribeFlowable(session, chat, sb, responseFlowable);
        } else {
            applicationEventPublisher.publishEvent(new MessageSendEvent(new ChatMessage(payload, chat, ChatSender.USER)));
            Flowable<ChatCompletionChunk> responseFlowable = openAiChatService.generateStreamResponse(chat, payload);
            subscribeFlowable(session, chat, sb, responseFlowable);
        }
    }

    private boolean hasStartFlag(String payload) {
        return payload.contains(START_CHAT_FLAG);
    }

    private void saveChatInMap(String payload, String chatSessionId) {
        Long chatId = Long.parseLong(payload.replaceAll(START_CHAT_FLAG, ""));
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(CustomErrorCode.NOT_EXISTS_CHATROOM_IN_DB));
        cacheStore.cacheChatSessionIdAndChat(chatSessionId, chat);
    }

    private boolean hasDeepFlag(String payload) {
        return payload.contains(DEEP_QUESTION_FLAG);
    }

    private void subscribeFlowable(WebSocketSession session, Chat chat, StringBuilder sb, Flowable<ChatCompletionChunk> responseFlowable) {
        StringBuilder chunkBuffer = new StringBuilder();
        ObjectMapper objectMapper = new ObjectMapper();
        responseFlowable.subscribe(
                chunk -> {
                    try {
                        String response = chunk.getChoices().get(0).getMessage().getContent();
                        if (!hasStreamFinishFlag(response)) {
                            chunkBuffer.append(response);
                            sb.append(response);
                            if (chunkBuffer.length() >= 5) {
                                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(chunkBuffer.toString())));
                                chunkBuffer.setLength(0);
                            }
                            return;
                        }
                        if (hasBufferRemainingChunk(chunkBuffer)) {
                            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(chunkBuffer.toString())));
                            chunkBuffer.setLength(0);
                        }
                        applicationEventPublisher.publishEvent(new MessageSendEvent(new ChatMessage(sb.toString(), chat, ChatSender.AI)));
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(null)));
                        sb.setLength(0);

                        long end = System.currentTimeMillis();
                        log.info("streaming spend time={}", end - start);
                    } catch (Exception e) {
                        log.error("An error occurred while processing the flowable", e);
                    }
                },
                Throwable::printStackTrace
        );
    }

    private boolean hasStreamFinishFlag(String response) {
        return response == null;
    }

    private boolean hasBufferRemainingChunk(StringBuilder builder) {
        return builder.length() > 0;
    }
}