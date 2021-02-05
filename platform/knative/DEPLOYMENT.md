This sample app provides a simple `Hello` web app based on Spring Boot and Spring Cloud Function.

* Install Tanzu Serverless/Knative
  

* Deployment
  * Knative service deployed via:
      * Kubernetes `kubectl` CLI
      * Knative `kn` CLI


* Serverless use-cases:
  * [x] Deployment of containers with the KNative(kn) CLI 
  * [x] Scale-to-zero, automatically
  * [x] Allow versioning of deployments and snapshots (deployed codes and configurations)
  * [x] Executing a particular version of a function
  * [x] Blue-Green and Canary deployments
    * It can be done in K8s
    * How to do B/G with the KNative(kn) CLI
      * Dynamic traffic splitting
      * Use Octant UI Plugin
  * [x] Dynamic resource configurations (memory, CPU cycles, concurrency, etc)
  * [x] Load-testing functions
  * [x] Delete a deployed service

Build Options:
* JVM application, leveraging OpenJDK
* Native Application, leveraging GraalVM

# Install Tanzu Serverless

Info URL: https://tanzu.vmware.com/serverless

Download: 
* log in to the Tanzu Network - https://network.pivotal.io/ 
* download Tanzu Serverless 
* follow the README.md#install for installation in your preferred K8s cluster

Pre-requisites:
* Kubernetes
    * Requires Kubernetes 1.17+
* Required command line tools
    * kubectl (Version 1.18 or newer)
    * kapp (Version 0.34.0 or newer)
    * ytt (Version 0.30.0 or newer)
    * yq (Version 3.4.1 )
    * kn
* Octant
    * install Octant - optional (https://github.com/vmware-tanzu/octant)
    * install Octant Plugin for Knative (https://github.com/vmware-tanzu/octant-plugin-for-knative)
* DNS
    * install Magic DNS - optional, easy to set up (https://knative.dev/v0.18-docs/install/any-kubernetes-cluster/)


### Summarized installation - subject to change
```shell
# Ensure that your Kubernetes client targets the intended cluster:
$ kubectl cluster-info

# Create a Kubernetes registry credentials secret in order to pull images:
# Your Tanzu Network login credentials
$ TANZU_LOGIN=user@example.com
$ TANZU_PASSWORD=password

# create secret
$ export SECRET_NAME=registry-credentials
$ export SECRET_NS=hello-function
$ kubectl create secret docker-registry ${SECRET_NAME} -n ${SECRET_NS} \
    --docker-server=registry.pivotal.io \
    --docker-username=${TANZU_LOGIN} --docker-password=${TANZU_PASSWORD}

# From the `serverless` directory, run the installation script:
$ ./bin/install-serverless.sh
```

# Deployment and Serverless use-cases

To start deploying without having to build the images, all have been made in DockerHub:
```shell
$ docker pull triathlonguy/hello-function:jvm
$ docker pull triathlonguy/hello-function:native

$ docker pull triathlonguy/hello-function:blue
$ docker pull triathlonguy/hello-function:green
```

To start, secrets have to be set up, to pull the images:
```shell
$ kubectl cluster-info

$ export WORKLOAD_NS=hello-function
$ kubectl create namespace ${WORKLOAD_NS}

# copy the registry credentials secret in namespace
# use yq v3!
$ kubectl get secret ${SECRET_NAME} -n ${SECRET_NS} -oyaml |  \
     yq d - 'metadata.creationTimestamp' | yq d - 'metadata.namespace' |  \
     yq d - 'metadata.resourceVersion' |  yq d - 'metadata.selfLink' | yq d - 'metadata.uid' |   kubectl apply -n $WORKLOAD_NS -f -

# Add the secret to the default service account in the namespace
$ kubectl patch serviceaccount -n ${WORKLOAD_NS} default -p '{"imagePullSecrets": [{"name": "'${SECRET_NAME}'"}]}'
```

## Deployment via Kubernetes `kubectl` CLI
The Knative service can be deployed using the `kubectl` CLI, similar to any K8s deployment.

```yaml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  creationTimestamp: null
  name: hello-function  <-- function name
  namespace: hello-function <-- namespace 
spec:
  template:
    metadata:
      annotations:
        client.knative.dev/user-image: triathlonguy/hello-function:jvm <-- image
      creationTimestamp: null
      name: hello-function-v1 <-- revision name
    spec:
      containerConcurrency: 25
      containers:
      - env:  <-- env variables
        - name: TARGET
          value: from Serverless Test - Spring Function on JVM
        image: triathlonguy/hello-function:jvm
        name: user-container
        readinessProbe:
          successThreshold: 1
          tcpSocket:
            port: 0
        resources: {}
      enableServiceLinks: false
      timeoutSeconds: 300
status: {}
```

```shell
# deploy 
$ kubectl apply -f platform/knative/kubernetes/service.yml

$ kn service list  -A
NAMESPACE              NAME                 URL                                                                 LATEST                  AGE     CONDITIONS   READY   REASON
hello-function         hello-function       http://hello-function.hello-function.35.184.97.2.xip.io             hello-function-v1       6m18s   3 OK / 3     True    
...

# cleanup
$ k delete -f platform/knative/kubernetes/service.yml
```

## Deployment via Knative `kn` CLI
```shell
# create service
$ kn service create hello-function -n hello-function --image triathlonguy/hello-function:jvm --env TARGET="from Serverless Test - Spring Function on JVM" --revision-name hello-function-v1

        Creating service 'hello-function' in namespace 'hello-function':
        0.178s The Route is still working to reflect the latest desired specification.
        0.195s Configuration "hello-function" is waiting for a Revision to become ready.
        20.967s ...
        21.077s Ingress has not yet been reconciled.
        21.094s Waiting for Envoys to receive Endpoints data.
        21.477s Waiting for load balancer to be ready
        21.706s Ready to serve.

Service 'hello-function' created to latest revision 'hello-function-v1'; it is available at URL:
http://hello-function.hello-function.35.184.97.2.xip.io

# get the external address for your ingress
kubectl get service envoy -n contour-external \
  --output 'jsonpath={.status.loadBalancer.ingress[0].ip}'
ex.: 35.184.97.2

# test the service
$ curl -w'\n' -H 'Content-Type: text/plain' http://hello-function.hello-function.35.184.97.2.xip.io -d "test"

# load test the service with Siege
# install on Mac with `brew install siege` 
$ siege -d1  -c50 -t10S  --content-type="text/plain" 'http://hello-function.hello-function.35.184.97.2.xip.io POST test'
```

## Automatic scale-to-zero
```shell
# load test the service with Siege
# install on Mac with `brew install siege` 
$ siege -d1  -c200 -t60S  --content-type="text/plain" 'http://hello-function.hello-function.35.184.97.2.xip.io POST from-my-function'

# observe the function instances scaling up and, after 60s of inactivity, terminate and scale all the way back to zero.
```

### Create a service revision
When creating a revision, if the `traffic` parameter is not specified, all traffic will be routed to the new revision, which automatically becomes the `@latest`.
Check the traffic allocation from the initial service creation with `revision-name=hello-function-v1`.

Note that the deployment is done automatically by KNative with zero-downtime, using a `blue-green deployment` pattern!
```shell
# revision hello-function-1 gets 100% of the traffic
$ kn service describe hello-function -n hello-function
        Name:       hello-function
        Namespace:  hello-function
        Age:        3m
        URL:        http://hello-function.hello-function.35.184.97.2.xip.io

        Revisions:  
          100%  @latest (hello-function-v1) [1] (3m)
                Image:  triathlonguy/hello-function:jvm (pinned to ef7bef)

        Conditions:  
          OK TYPE                   AGE REASON
          ++ Ready                   3m 
          ++ ConfigurationsReady     3m 
          ++ RoutesReady             3m 
```

To create a new revision, however maintain the traffic on the previous revision, set the `traffic` parameter and prevent the new deployment to use the new revision.
This allow a `blue-green` deployment type of testing, as shown in future paragraphs.
```shell
# create revision hello-function-v2
$ kn service update hello-function -n hello-function --image triathlonguy/hello-function:jvm --env TARGET="from Serverless Test - from revision 2 of Spring Function on JVM" --revision-name hello-function-v2 --traffic @latest=0,hello-function-v1=100

Updating Service 'hello-function' in namespace 'hello-function':

  0.065s The Route is still working to reflect the latest desired specification.
  0.126s Revision "hello-function-v2" is not yet ready.
  5.596s ...
  5.679s Ingress has not yet been reconciled.
  5.758s ...
  5.911s unsuccessfully observed a new generation
  6.002s Waiting for Envoys to receive Endpoints data.
  7.735s Waiting for load balancer to be ready
  7.891s Ready to serve.

Service 'hello-function' updated to latest revision 'hello-function-v2' is available at URL:
http://hello-function.hello-function.35.184.97.2.xip.io
```

KN CLI allows us to `describe` the service and indicate that traffic still goes to the previous revision, while the new one gets zero traffic
```shell
$ kn service describe hello-function -n hello-function
Name:       hello-function
Namespace:  hello-function
Age:        12m
URL:        http://hello-function.hello-function.35.184.97.2.xip.io

Revisions:  
     +  hello-function-v2 (current @latest) [2] (1m)
        Image:  triathlonguy/hello-function:jvm (pinned to ef7bef)
  100%  hello-function-v1 [1] (12m)
        Image:  triathlonguy/hello-function:jvm (pinned to ef7bef)

Conditions:  
  OK TYPE                   AGE REASON
  ++ Ready                   1m 
  ++ ConfigurationsReady     1m 
  ++ RoutesReady             1m 
```

### Executing a specific revision of a function

To access a specific revision of a service, routing can be set up by assigning a tag to the revision (see https://knative.dev/docs/serving/using-subroutes/).

A tag applied to a route leads to an address for the specific traffic target to be created.
You can access that specific revision by prefixing `tag-` to the route.

For example, let's update revision `hello-function-v2` and assign the tag `candidate` to the revision. 
This allows us to test this candidate revision before sending any traffic to it.
```shell
$ kn service update hello-function -n hello-function  --tag hello-function-v2=candidate

$ kubectl get svc  -n hello-function 
NAME                                              TYPE           CLUSTER-IP    EXTERNAL-IP                                PORT(S)                             AGE
candidate-hello-function                          ExternalName   <none>        envoy.contour-internal.svc.cluster.local   80/TCP                              85s
...

$ curl -w'\n' -H 'Content-Type: text/plain' http://candidate-hello-function.hello-function.35.184.97.2.xip.io -d "test"

Hello from Serverless Test - from revision 2 of Spring Function on JVM
```

## Blue-Green and Canary deployments

### It can be done in K8s
Blue-green deployment can be done in K8s, as shown in the following Tanzu Developer Center Blog Post: [Declarative Deployments in Kubernetes: What Options Do I Have?](https://github.com/ddobrin/declarative-deployments-k8s#5)

### How to do B/G with the KNative(kn) CLI
When creating a revision, if the `traffic` parameter is not specified, all traffic will be routed to the new revision, which automatically becomes the `@latest`.
Check the traffic allocation from the initial service creation with `revision-name=hello-function-v1`.

To avoid the automatic traffic re-routing, a new revision should be created `without traffic routed to it` and `while specifying tag` (see above for both), in order to allow testing of the `green` version, and changing `blue` to `green` at a later time, after testing.

Let's reset the test by deploying a new image, as revision 3 and label it as the `blue` version, stable, and with traffic routed to it.
`green` will follow :
```shell
$ kn service update hello-function -n hello-function --image triathlonguy/hello-function:blue --env TARGET="from Serverless Test - from revision BLUE of Spring Function on JVM" --revision-name hello-function-blue --traffic @latest=100 --tag hello-function-blue=stable

## testing the BLUE revision
$ curl -w'\n' -H 'Content-Type: text/plain' http://stable-hello-function.hello-function.35.184.97.2.xip.io -d "test"
Hello from Serverless Test - from revision BLUE of Spring Function on JVM

## deploy the GREEN revision, with tag green-candidate, and image tag green
$ kn service update hello-function -n hello-function --image triathlonguy/hello-function:green --env TARGET="from Serverless Test - from revision GREEN of Spring Function on JVM" --revision-name hello-function-green --traffic @latest=0,hello-function-blue=100 --tag hello-function-green=green-candidate

## testing the GREEN revision
$ curl -w'\n' -H 'Content-Type: text/plain' http://green-candidate-hello-function.hello-function.35.184.97.2.xip.io -d "test"
Hello from Serverless Test - from revision GREEN of Spring Function on JVM

## Switch traffic from BLUE to GREEN after testing of GREEN has been completed

# first, we untag the stable tag from the BLUE revision 
# function route still points to BLUE
$ kn service update hello-function -n hello-function --untag stable

$ curl -w'\n' -H 'Content-Type: text/plain' http://hello-function.hello-function.35.184.97.2.xip.io -d "test"
Hello from Serverless Test - from revision BLUE of Spring Function on JVM

## switch traffic to GREEN and assign green as STABLE
kn service update hello-function -n hello-function --tag hello-function-green=stable --traffic hello-function-green=100

## traffic points to the new route
$ curl -w'\n' -H 'Content-Type: text/plain' http://hello-function.hello-function.35.184.97.2.xip.io -d "test"
Hello from Serverless Test - from revision GREEN of Spring Function on JVM

## testing by revision, the STABLE tag points to the GREEN revision
$ curl -w'\n' -H 'Content-Type: text/plain' http://stable-hello-function.hello-function.35.184.97.2.xip.io -d "test"
Hello from Serverless Test - from revision GREEN of Spring Function on JVM
```

### Canary deployment 
For canary deployments, when deploying a new revision, traffic can be set to a small percentage, tested, then the canary can become the new version
```shell
# deploy the canary version
$ kn service update hello-function -n hello-function --image triathlonguy/hello-function:jvm --env TARGET="from Serverless Test - from revision CANARY of Spring Function on JVM" --revision-name hello-function-canary --traffic @latest=10,hello-function-green=90 --tag hello-function-canary=canary

# testing will respect the traffic percentage set above
$ curl -w'\n' -H 'Content-Type: text/plain' http://hello-function.hello-function.35.184.97.2.xip.io -d "test"
Hello from Serverless Test - from revision GREEN of Spring Function on JVM
$ curl -w'\n' -H 'Content-Type: text/plain' http://hello-function.hello-function.35.184.97.2.xip.io -d "test"
Hello from Serverless Test - from revision CANARY of Spring Function on JVM
$ curl -w'\n' -H 'Content-Type: text/plain' http://hello-function.hello-function.35.184.97.2.xip.io -d "test"
Hello from Serverless Test - from revision GREEN of Spring Function on JVM
...

# traffic can be routed 100% to the canary revision when testing is complete
kn service update hello-function -n hello-function --traffic @latest=100

# traffic is routed only to the latest revision, which was canary
$ curl -w'\n' -H 'Content-Type: text/plain' http://hello-function.hello-function.35.184.97.2.xip.io -d "test"
Hello from Serverless Test - from revision CANARY of Spring Function on JVM
```

Revisions can be listed as follows:
```shell
# KNative
$ kn service describe hello-function -n hello-function
Name:       hello-function
Namespace:  hello-function
Age:        2h
URL:        http://hello-function.hello-function.35.184.97.2.xip.io

Revisions:  
     +  hello-function-canary (current @latest) #canary [5] (26m)
        Image:  triathlonguy/hello-function:jvm (pinned to ef7bef)
  100%  @latest (hello-function-canary) [5] (26m)
        Image:  triathlonguy/hello-function:jvm (pinned to ef7bef)
     +  hello-function-green #green-candidate [4] (45m)
        Image:  triathlonguy/hello-function:green (pinned to ef7bef)
     +  hello-function-green #stable [4] (45m)
        Image:  triathlonguy/hello-function:green (pinned to ef7bef)
     +  hello-function-v2 #candidate [2] (1h)
        Image:  triathlonguy/hello-function:jvm (pinned to ef7bef)

Conditions:  
  OK TYPE                   AGE REASON
  ++ Ready                   4m 
  ++ ConfigurationsReady    26m 
  ++ RoutesReady             4m 

# Kubernetes
$ kubectl get deploy -n hello-function
NAME                                                 READY   UP-TO-DATE   AVAILABLE   AGE
hello-function-blue-deployment                       0/0     0            0           52m
hello-function-canary-deployment                     0/0     0            0           25m
hello-function-green-deployment                      0/0     0            0           45m
hello-function-native-hello-function-v1-deployment   0/0     0            0           4h10m
hello-function-v1-deployment                         0/0     0            0           126m
hello-function-v2-deployment                         0/0     0            0           115m
```

# Setting requests and limits dynamically
```shell
# create the service with requests and limits
$ kn service create hello-limits -n hello-function --image triathlonguy/hello-function:jvm --env TARGET="from Serverless Test - with limits" --revision-name hello-limits-v1 --request memory=200Mi,cpu=200m --limit cpu=450m

# update the service with requests and limits dynamically
$ kn service update hello-limits -n hello-function --limit cpu=450m,memory=1Gi

# generated YAML shows the limits set above when creating the service, as an example
---
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  annotations:
...
spec:
  template:
    metadata:
      annotations:
        client.knative.dev/user-image: triathlonguy/hello-function:jvm
      creationTimestamp: null
      name: hello-limits-v1
    spec:
      containerConcurrency: 0
      containers:
        - env:
            - name: TARGET
              value: from Serverless Test - with limits
          image: triathlonguy/hello-function:jvm
          name: user-container
          readinessProbe:
            successThreshold: 1
            tcpSocket:
              port: 0
          resources:
            limits:
              cpu: 450m
            requests:
              cpu: 200m
              memory: 200Mi
      enableServiceLinks: false
      timeoutSeconds: 300
  traffic:
    - latestRevision: true
      percent: 100
status:
  address:
    url: http://hello-limits.hello-function.svc.cluster.local
...
  traffic:
    - latestRevision: true
      percent: 100
      revisionName: hello-limits-v1
  url: http://hello-limits.hello-function.35.184.97.2.xip.io
```

### Setting auto-scaling dynamically
```shell
# auto-scale up when the concurrent number of requests in the container hits 50
$ kn service update hello-limits -n hello-function --concurrency-limit 50

# load-test with Siege
$ siege  -c200 -t20S  --content-type="text/plain" 'http://hello-limits.hello-function.35.184.97.2.xip.io POST test'

# YAML change indicates the limit for concurrency
spec:
  containerConcurrency: 50
  containers:
    - env:
        - name: TARGET
          value: from Serverless Test - with limits
      image: index.docker.io/triathlonguy/hello-function@sha256:ef7bef1e145f85ff9e34ad12c163b91cc4dcc6e5c28d1c02052b184131155f01
```

## Delete a deployed Service
The service can be deleted from the KN CLI:
```shell
$ kn service delete hello-function -n hello-function
```
