package com.ga.medibook.notification;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseNotificationService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String email) {

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitters.put(email, emitter);

        emitter.onCompletion(() -> emitters.remove(email));
        emitter.onTimeout(() -> emitters.remove(email));
        emitter.onError(error -> emitters.remove(email));

        return emitter;
    }

    public void sendNotification(String email, String message) {

        SseEmitter emitter = emitters.get(email);

        if (emitter == null) {
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("notification")
                            .data(message)
            );
        } catch (IOException e) {
            emitters.remove(email);
            emitter.complete();
        }
    }
}