package com._5ganalysisrate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@CrossOrigin
public class FiveAnalysisSystemApplication {

    private static final Logger log = LoggerFactory.getLogger(FiveAnalysisSystemApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(FiveAnalysisSystemApplication.class, args);
    }
     @Autowired
   private RequestMappingHandlerMapping requestMappingHandlerMapping;
   
   @PostConstruct
   public void init() {
       requestMappingHandlerMapping.getHandlerMethods().forEach((key, value) -> {
           log.info("Mapped URL path [{}] onto method [{}]", key, value);
       });
   }

}
