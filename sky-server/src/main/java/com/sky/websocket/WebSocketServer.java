package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    private static Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        log.info("客户端 {} 建立连接", sid);
        sessions.put(sid, session);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到来自客户端 {} 的信息：{}", sid, message);
    }

    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        log.info("客户端 {} 的连接断开", sid);
        sessions.remove(sid);
    }

    /**
     * 群发消息给所有连接的客户端
     * @param message
     */
    public void sendMessageToAllClient(String message) {
        try {
            for (Session session : sessions.values()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            log.error("群发消息错误：" + e.getMessage());
            e.printStackTrace();
        }
    }

}
