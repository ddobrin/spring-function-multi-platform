package com.example.hello;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.springframework.cloud.function.adapter.azure.AzureSpringBootRequestHandler;

import java.util.Optional;

public class HelloFunctionHandler extends AzureSpringBootRequestHandler<String, String> {
    @FunctionName("hello")
    public HttpResponseMessage execute(
            @HttpTrigger(name = "request", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {
        String message = request.getBody().get();

        context.getLogger().info(new StringBuilder().append("Function: ").append(context.getFunctionName()).append(" is saying hello ").append(message).toString());

        return request
                .createResponseBuilder(HttpStatus.OK)
                .body(handleRequest(message, context))
                .header("Content-Type", "text/plain")
                .build();
    }
}
