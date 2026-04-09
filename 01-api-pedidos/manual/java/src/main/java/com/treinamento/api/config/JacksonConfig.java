package com.treinamento.api.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.Module;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Module javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_FORMAT));
        return module;
    }

    @Bean
    public Module numberModule() {
        SimpleModule module = new SimpleModule();
        // Serializa Double/Float sem decimais quando o valor e inteiro (ex: 4500.0 -> 4500)
        module.addSerializer(Double.class, new WholeNumberSerializer());
        return module;
    }

    /**
     * Serializa Double sem trailing zeros quando o valor e um numero inteiro.
     * Ex: 4500.0 -> 4500, 4500.5 -> 4500.5
     */
    public static class WholeNumberSerializer extends StdSerializer<Double> {
        public WholeNumberSerializer() {
            super(Double.class);
        }

        @Override
        public void serialize(Double value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            if (value == Math.floor(value) && !Double.isInfinite(value)) {
                // Numero inteiro - escrever como long
                gen.writeNumber(value.longValue());
            } else {
                gen.writeNumber(value);
            }
        }
    }
}
