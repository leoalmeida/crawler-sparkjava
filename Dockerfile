FROM maven:3.8.5-openjdk-17-slim

ADD . /usr/src/sparkjava
WORKDIR /usr/src/sparkjava
EXPOSE 8081
ENTRYPOINT ["mvn", "clean", "install", "exec:java"]