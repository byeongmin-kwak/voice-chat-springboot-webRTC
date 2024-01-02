package voice_chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@RestController
public class SignalingHandler {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    // WebSocket 핸들러: Offer 정보 주고 받기
    @MessageMapping("/peer/offer/{voiceKey}/{roomId}")
    @SendTo("/topic/peer/offer/{voiceKey}/{roomId}")
    public String PeerHandleOffer(@Payload String offer, @DestinationVariable(value = "roomId") String roomId,
                                  @DestinationVariable(value = "voiceKey") String voiceKey) {
        log.info("offer: {}", offer);
        return offer;
    }

    // WebSocket 핸들러: IceCandidate 정보 주고 받기
    @MessageMapping("/peer/iceCandidate/{voiceKey}/{roomId}")
    @SendTo("/topic/peer/iceCandidate/{voiceKey}/{roomId}")
    public String PeerHandleIceCandidate(@Payload String candidate, @DestinationVariable(value = "roomId") String roomId,
                                         @DestinationVariable(value = "voiceKey") String voiceKey) {
        log.info("candidate: {}", candidate);
        return candidate;
    }

    // WebSocket 핸들러: Answer 정보 주고 받기
    @MessageMapping("/peer/answer/{voiceKey}/{roomId}")
    @SendTo("/topic/peer/answer/{voiceKey}/{roomId}")
    public String PeerHandleAnswer(@Payload String answer, @DestinationVariable(value = "roomId") String roomId,
                                   @DestinationVariable(value = "voiceKey") String voiceKey) {
        log.info("answer: {}", answer);
        return answer;
    }

    // WebSocket 핸들러: Key를 받기위해 신호 보내기
    @MessageMapping("/call/key")
    @SendTo("/topic/call/key")
    public String callKey(@Payload String message) {
        log.info("call message: {}", message);
        return message;
    }

    // WebSocket 핸들러: 자신의 Key를 모든 연결된 세션에 보내기
    @MessageMapping("/send/key")
    @SendTo("/topic/send/key")
    public String sendKey(@Payload String message) {
        log.info("send message: {}", message);
        return message;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 세션 ID 추출
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket 세션이 연결되었습니다. - 세션 ID: {}", sessionId);

        Map<String, String> message = new HashMap<>();
        message.put("type", "connected");
        message.put("sessionId", sessionId);

        // 일정 시간(예: 1초) 지연 후 세션 ID 전송
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Map<String, String> message = new HashMap<>();
                message.put("type", "connected");
                message.put("sessionId", sessionId);

                // 클라이언트에게 세션 ID 전송
                messagingTemplate.convertAndSend("/topic/user/connected", message);
            }
        }, 1000); // 1000 밀리초 = 1초
    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 세션 ID 추출
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket 세션이 끊겼습니다. - 세션 ID: {}", sessionId);

        // 연결 끊김을 다른 사용자에게 알리기 위해 JSON 메시지 보내기
        Map<String, String> message = new HashMap<>();
        message.put("type", "disconnected");
        message.put("sessionId", sessionId);
        messagingTemplate.convertAndSend("/topic/user/disconnected", message);
    }
}
