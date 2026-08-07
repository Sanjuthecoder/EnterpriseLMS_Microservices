package com.edtech.lms.telemetry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@Configuration
@EnableMongoRepositories(basePackages = "com.edtech.lms.telemetry.repositories")
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Bean
    public MongoTemplate mongoTemplate(org.springframework.data.mongodb.MongoDatabaseFactory databaseFactory, 
                                       org.springframework.data.mongodb.core.convert.MappingMongoConverter converter) {
        converter.setCustomConversions(mongoCustomConversions());
        return new MongoTemplate(databaseFactory, converter);
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
            new JsonNodeReadConverter(),
            new JsonNodeWriteConverter()
        ));
    }

    @ReadingConverter
    public static class JsonNodeReadConverter implements Converter<Object, JsonNode> {
        private final ObjectMapper objectMapper;

        public JsonNodeReadConverter() {
            this.objectMapper = new ObjectMapper();
            this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        }

        @Override
        public JsonNode convert(Object source) {
            try {
                if (source instanceof org.bson.Document) {
                    return objectMapper.readTree(((org.bson.Document) source).toJson());
                } else if (source instanceof java.util.List) {
                    return objectMapper.valueToTree(source);
                } else if (source instanceof String) {
                    return objectMapper.readTree((String) source);
                }
                return objectMapper.valueToTree(source);
            } catch (Exception e) {
                return null;
            }
        }
    }

    @WritingConverter
    public static class JsonNodeWriteConverter implements Converter<JsonNode, Object> {
        private final ObjectMapper objectMapper;

        public JsonNodeWriteConverter() {
            this.objectMapper = new ObjectMapper();
            this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        }

        @Override
        public Object convert(JsonNode source) {
            try {
                if (source.isArray()) {
                    return objectMapper.readValue(source.toString(), java.util.List.class);
                }
                return objectMapper.readValue(source.toString(), java.util.Map.class);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
