package com.mkyuan.fountainbase.agent.chatbot.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ChatRunningStatus {
    private final Logger logger = LogManager.getLogger(this.getClass());
    // 使用ConcurrentHashMap存储每个用户的聊天状态
    private final Map<String, AtomicBoolean> userRunningStatus = new ConcurrentHashMap<>();

    /**
     * 检查指定用户的聊天是否正在运行
     * @param userName 用户名
     * @return 是否运行中
     */
    public boolean isRunning(String userName) {
        // 如果用户不存在，默认为运行状态
        return userRunningStatus.computeIfAbsent(userName, k -> new AtomicBoolean(true)).get();
    }

    /**
     * 设置指定用户的聊天运行状态
     * @param userName 用户名
     * @param running 运行状态
     */
    public void setRunning(String userName, boolean running) {
        AtomicBoolean status = userRunningStatus.computeIfAbsent(userName, k -> new AtomicBoolean(true));
        boolean oldValue = status.getAndSet(running);
        if (oldValue != running) {
            logger.info("用户[{}]聊天运行状态已更改: {} -> {}", userName, oldValue, running);
        }
    }

    /**
     * 重置用户的聊天状态为运行中
     * @param userName 用户名
     */
    public void resetStatus(String userName) {
        userRunningStatus.computeIfAbsent(userName, k -> new AtomicBoolean(true)).set(true);
        logger.info("用户[{}]聊天状态已重置为运行中", userName);
    }

    /**
     * 清理用户的聊天状态（可选，用于用户会话结束时）
     * @param userName 用户名
     */
    public void clearStatus(String userName) {
        userRunningStatus.remove(userName);
        logger.info("用户[{}]聊天状态已清理", userName);
    }

}
