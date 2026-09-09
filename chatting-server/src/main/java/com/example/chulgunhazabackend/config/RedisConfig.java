package com.example.chulgunhazabackend.config;

import com.example.chulgunhazabackend.websocket.ChatFanoutSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

// #101: user-server의 RedisConfig와 같은 방식(LettuceConnectionFactory(host, port))으로
// 같은 Redis 인스턴스에 붙는다 — refresh 토큰(user-server)과 채팅 presence/fanout은
// 키 프리픽스가 달라서(chat:presence:*, chat:fanout) 충돌하지 않는다.
// StringRedisTemplate: ChatPresenceRegistry가 문자열 키만 쓰므로 이걸로 충분.
// RedisMessageListenerContainer: ChatFanoutSubscriber를 chat:fanout 채널에 구독시킨다.
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        return new StringRedisTemplate(redisConnectionFactory());
    }

    // #101: ChatFanoutSubscriber를 이 클래스의 생성자 필드(@RequiredArgsConstructor)로
    // 받았더니 순환 참조가 났다 — ChatFanoutSubscriber → WebSocketMessageHandler →
    // ChatPresenceRegistry → StringRedisTemplate(바로 이 RedisConfig가 만드는 빈)인데,
    // RedisConfig 자신을 만드는 데 ChatFanoutSubscriber가 먼저 필요하다고 하면 그
    // 체인이 자기 자신으로 되돌아온다(실측: UnsatisfiedDependencyException, "Is there
    // an unresolvable circular reference?"). @Bean 메서드의 파라미터로만 받으면
    // RedisConfig 인스턴스 자체는 host/port만으로 먼저 만들어지고, ChatFanoutSubscriber는
    // 이 특정 빈을 만들 때가 되어서야(그때는 StringRedisTemplate 빈 정의가 이미 등록된
    // 뒤라) 필요해지므로 순환이 끊긴다.
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(ChatFanoutSubscriber chatFanoutSubscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory());
        container.addMessageListener(chatFanoutSubscriber, new ChannelTopic(ChatFanoutSubscriber.FANOUT_CHANNEL));
        return container;
    }
}
