package com.example.hello;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.function.adapter.azure.AzureSpringBootRequestHandler;

import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

public class HelloFunctionTests {

    @Test
    public void test() {
        String result = new HelloFunction().hello().apply("test");
        assertThat(result).isEqualTo("Hello: test, Source: null");
    }

    @Test
    public void start() throws Exception {
        AzureSpringBootRequestHandler<String, String> handler = new AzureSpringBootRequestHandler<>(
                HelloFunction.class);
        ExecutionContext ec = new ExecutionContext() {
            @Override
            public Logger getLogger() {
                return Logger.getAnonymousLogger();
            }

            @Override
            public String getInvocationId() {
                return "id3";
            }

            @Override
            public String getFunctionName() {
                return "hello";
            }
        };

        String result = handler.handleRequest("test", ec);
        handler.close();
        assertThat(result).isEqualTo("Hello: test, Source: from-function");
    }
}
