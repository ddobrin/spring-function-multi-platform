This sample app provides a simple `Hello` web app based on Spring Boot and Spring Cloud Function.

* Deployment
  * Kubernetes `kubectl` CLI
 
Build Options:
* JVM application, leveraging OpenJDK
* Native Application, leveraging GraalVM

# Deployment 

To start deploying without having to build the images, all have been made available in DockerHub:
```shell
$ docker pull triathlonguy/hello-function:jvm
$ docker pull triathlonguy/hello-function:native

$ docker pull triathlonguy/hello-function:blue
$ docker pull triathlonguy/hello-function:green
```

## Deployment via Kubernetes `kubectl` CLI
The app can be deployed using the `kubectl` CLI, similar to any K8s deployment.

Deploy with a single replica, in the default namespace:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app.kubernetes.io/name: hello-function
  name: hello-function-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: hello-function
  template:
    metadata:
      labels:
        app.kubernetes.io/name: hello-function
    spec:
      containers:
        - image: triathlonguy/hello-function:jvm
          imagePullPolicy: IfNotPresent
          env:
            - name: TARGET
              value: from-Kubernetes-deployed-function
          name: app
```

Expose the service using a LoadBalancer:
```shell
apiVersion: v1
kind: Service
metadata:
  labels:
    app.kubernetes.io/name: hello-function
  name: hello-function-service
spec:
  ports:
  - port: 80
    targetPort: 8080
  selector:
    app.kubernetes.io/name: hello-function
  sessionAffinity: None
  type: LoadBalancer
```

```shell
# deploy 
$ kubectl apply -f platform/kubernetes/app/

# validate the deployment
$ kubectl get deploy | grep hello
hello-function-app                    1/1     1            1           2m44s

# validate that the service is available externally
$ kubectl get svc | grep hello
hello-function-service                    LoadBalancer   10.0.23.136   34.71.24.80   80:32500/TCP                 3m49s

# send a cUrl request to validate
$ curl -w'\n' -H 'Content-Type: text/plain' 34.71.24.80 -d "from a Function"
Hello: from a Function, Source: from-Kubernetes-deployed-function
...

# cleanup
$ kubectl delete -f platform/kubernetes/app/
```
