package com.example.chulgunhazabackend.event.common;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class EventConfiguration {

    private final ApplicationContext context;
    @Bean
    public InitializingBean eventsInitializer(){
        return () -> Events.setPublisher(context);
    }


}
