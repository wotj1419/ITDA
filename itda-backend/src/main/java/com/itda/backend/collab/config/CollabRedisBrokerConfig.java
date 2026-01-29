package com.itda.backend.collab.config;

import com.itda.backend.collab.service.CollabRedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@RequiredArgsConstructor
public class CollabRedisBrokerConfig {

    private final CollabRedisSubscriber subscriber;

    @Bean
    public ChannelTopic collabRedisTopic(@Value("${collab.redis.channel:collab:ws}") String channel) {
        return new ChannelTopic(channel);
    }

    @Bean
    public RedisMessageListenerContainer collabRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChannelTopic collabRedisTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new MessageListenerAdapter(subscriber), collabRedisTopic);
        return container;
    }
}
