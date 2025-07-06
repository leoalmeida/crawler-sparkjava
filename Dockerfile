FROM maven:3.6.3-jdk-14

ADD . /usr/src/sparkjava
WORKDIR /usr/src/sparkjava
EXPOSE 4567
ENTRYPOINT ["mvn", "clean", "verify", "exec:java"]