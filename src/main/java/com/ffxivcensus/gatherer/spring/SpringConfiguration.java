package com.ffxivcensus.gatherer.spring;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import com.ffxivcensus.gatherer.config.ApplicationConfig;
import com.ffxivcensus.gatherer.config.ConfigurationBuilder;

@Configuration
public class SpringConfiguration {

    @Bean
    public ApplicationConfig applicationConfig() throws ParserConfigurationException, IOException, SAXException {
        return ConfigurationBuilder.createBuilder()
                                   .loadXMLConfiguration()
                                   .getConfiguration();
    }
}