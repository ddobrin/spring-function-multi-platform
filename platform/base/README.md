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
* OpenJDK version "11.0.10" 2021-01-19
* OpenJDK 64-Bit Server VM GraalVM CE 21.0.0 (build 11.0.10+8-jvmci-21.0-b06, mixed mode, sharing)

Deployment Models:
* Standalone web app
* Kubernetes Deployment and Service
* Knative Service

Source code tree:
```
src/
├── main
│   ├── java
│   │   └── com
│   │       └── example
│   │           └── hello
│   │               └── HelloFunction.java
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

# The function used in this app is available in HelloFunction.java
```

# Build

Building the code with the Spring Boot Maven wrapper leverages the following profiles:
* native-image - build a Spring Native image leveraging GraalVM
* jvm-image - build a Spring JVM-based image leveraging OpenJDK

Building an executable application with the GraalVM compiler leverages the profile and requires the installation of the GraalVM and the native-image builder utility:
* native

## Build code as a JVM app - uses Spring Boot Maven plugin, embedded Netty HTTP server
```bash 
# build and run code using
$ ./mvnw clean spring-boot:run

# run with default app properties
./mvnw clean package spring-boot:run

# test locally
$ curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Function"
Hello: from a Function, Source: from-function

# test with env vars
./mvnw clean package spring-boot:run -Dspring-boot.run.arguments=--TARGET=localtest

# test locally
$ curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Function"
Hello: from a Function, Source: localtest
```

## Build code as a Native app - uses the GraalVM compiler, embedded Netty HTTP server
```bash 
# switch to the GraalVM JDK for this build
# ex, when using SDKman
$ sdk use java 21.0.0.r11-grl

# build and run code using
$ ./mvnw clean package -Pnative

# start the native executable
$ ./target/hello-function

# test locally
$ curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Function"
Hello: from a Function, Source: from-function
```

## Build code as a JVM image using the Spring Boot Maven plugin
```bash 
# build image with <default> configuration
$ ./mvnw clean spring-boot:build-image

# test with Docker
docker run -p 8080:8080 hello-function:0.0.1

$ curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Function"
Hello: from a Function, Source: from-function

# test with Docker and env vars
docker run -e TARGET=dockertest -p 8080:8080 hello-function:0.0.1

$ curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Function"
Hello: from a Function, Source: dockertest

#--------------------------------------------------------------
# build JVM image with the CNB Paketo buildpack of your choice
$ ./mvnw clean spring-boot:build-image -Pjvm-image

# start Docker image
$ docker run -e TARGET=docker-jvm-image-test -p 8080:8080 hello-function:jvm

# test Docker image locally
$ curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Function"
Hello: from a Function, Source: docker-jvm-image-test
```

## Build code as a Spring Native image using the Spring Boot Maven plugin and the Java Native Paketo Buildpacks
```bash 
# build image with the CNB Paketo buildpack of your choice
$ ./mvnw clean spring-boot:build-image -Pnative-image

# start Docker image
$ docker run -e TARGET=docker-native-image-test -p 8080:8080 hello-function:native

# test Docker image locally
$ curl -w'\n' -H 'Content-Type: text/plain' localhost:8080 -d "from a Function"
Hello: from a Function, Source: docker-native-image-test
```

