package org.example.hexorise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HexoriseApplication {

    public static void main(String[] args) {
        SpringApplication.run(HexoriseApplication.class, args);
    }

}
