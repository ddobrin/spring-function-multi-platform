This sample app provides a simple `Hello` web app based on Spring Boot and Spring Cloud Function.

Which topics are addressed in this repo:
* Build
  *  Build a JVM or Native image with Spring Boot and GraalVM
  *  Run locally

Build Options:
* JVM application, leveraging OpenJDK
* Native Application, leveraging GraalVM

Supported Versions:
* Spring Boot 2.4.1
* Spring Native 0.8.5
* OpenJDK version 1.8
* OpenJDK 64-Bit Server VM GraalVM CE 21.0.0 (build 11.0.10+8-jvmci-21.0-b06, mixed mode, sharing)

Deployment Models:
* Standalone app
* Azure Functions

Source code tree:
```
src/
├── azure
│   ├── host.json
│   └── local.settings.json
├── main
│   ├── java
│   │   └── com
│   │       └── example
│   │           └── hello
│   │               ├── HelloFunction.java
│   │               └── HelloFunctionHandler.java
│   └── resources
│       ├── application.properties
│       ├── static
│       └── templates
└── test
    └── java
        └── com
            └── example
                └── hello
                    └── HelloFunctionTests.java
```

# Build and Deploy locally

You can run these Azure functions locally, similar to other Spring Cloud Function samples, however this time by using the Azure Maven plugin, as the Microsoft Azure functions execution context must be available.

**Please note**: Java Functions supports only Java 8 as of beginning of Feb 2021, Java 11 is in preview.

## Build and package
```shell
# Build and package 
$ mvn clean package 

# Previously, for other examples
$ mvn spring-boot:run

# For Azure Functions
$ mvn clean package azure-functions:run

or 

$ mvn azure-functions:run
```

The `hello` function takes a text argument and uses the content type `text/plain`.
Azure Functions require a handler, provided in the HelloFunctionHandler, which defines the HttpTrigger for the function and inject the proper Azure context at runtime.


```shell
# testing with cURL
$ curl -H 'Content-Type: text/plain' http://localhost:7071/api/hello -d "test"
Hello: test, Source: from-function
```

# Deploy to Azure
To run locally on top of Azure Functions, and to deploy to your live Azure environment, you will need the Azure Functions Core Tools installed along with the Azure CLI (see https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-first-java-maven for more details).

To deploy the functions to your live Azure environment, including an automatic provisioning of an HTTPTrigger for the functions:
```shell
# login to Azure from the CLI
$ az login

# deploy the function
$ mvn azure-functions:deploy

[INFO] --- azure-functions-maven-plugin:1.3.4:deploy (default-cli) @ hello-function ---
[WARNING] Azure Functions only support JDK 8, which is lower than local JDK version 11.0.10
[INFO] Authenticate with Azure CLI 2.0
[INFO] Updating the specified function app...
[INFO] Java version of function host : 1.8
[INFO] Successfully updated the function app.hello-function-example-azure
[INFO] Trying to deploy the function app...
[INFO] Trying to deploy artifact to hello-function-example-azure...
[INFO] Successfully deployed the artifact to https://hello-function-example-azure.azurewebsites.net
[INFO] Successfully deployed the function app at https://hello-function-example-azure.azurewebsites.net
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------

# Note: 
# the deployment URL is: https://https://hello-function-example-azure.azurewebsites.net
# the function can be accessed at: https://function-sample-azure.azurewebsites.net/api/hello
```

On another terminal try this:
```shell
# testing
curl  -H 'Content-Type: text/plain' https://hello-function-example-azure.azurewebsites.net/api/hello -d "test"
Hello: test, Source: from-function
```
Please ensure that you use the right URL for the functions above.

Alternatively you can test the function in the Azure Dashboard UI:

* click on the Dashboard
* click on the function app `hello-function-example-azure`
* click on the left nav `Functions` and click the function name `hello`
* click on the left nav `Code and Test` and at the top of the page `Test/Run`
* Set the Http Method to Post
* In the body of the request, on the right-hand side, paste the same example we have used above:
```shell
"test"

# Output indicates
Hello: test, Source: from-function
```

Please note that the Dashboard provides by default information on Function Execution Count, Memory Consumption and Execution Time.

## Debug in a local environment
If you would like to debug the functions in your IDE, all you have to do is:
```shell
# start app locally
# enableDebug opens the debug port in the current host at port 5005
$ mvn azure-functions:run -DenableDebug
```

You can start a remote debugging session in any IDE at `localhost` and port `5005`, set breakpoints and send requests.
