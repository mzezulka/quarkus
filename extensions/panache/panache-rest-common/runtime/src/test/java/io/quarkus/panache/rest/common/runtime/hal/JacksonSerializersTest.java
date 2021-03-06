package io.quarkus.panache.rest.common.runtime.hal;

import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

class JacksonSerializersTest extends AbstractSerializersTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(HalEntityWrapper.class, new HalEntityWrapperJacksonSerializer(new BookHalLinksExtractor()));
        module.addSerializer(HalCollectionWrapper.class,
                new HalCollectionWrapperJacksonSerializer(new BookHalLinksExtractor()));
        objectMapper.registerModule(module);
    }

    @Override
    String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
