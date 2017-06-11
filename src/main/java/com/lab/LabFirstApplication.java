package com.lab;

import javax.annotation.PostConstruct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class LabFirstApplication extends SpringBootServletInitializer {

//    @Autowired
//    ViewRepository viewRepository;
    
    public static void main(String[] args) {
        SpringApplication.run(LabFirstApplication.class, args);
    }
    
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(LabFirstApplication.class);
    }
    
    @PostConstruct
    public void init() {
        // Empty
    }
}
