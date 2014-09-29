FROM dockerfile/java
MAINTAINER Matt Clark

ADD target/squink-0.1.0-SNAPSHOT-standalone.jar /squink/
ADD squink.conf.edn /data/


CMD ["java", "-jar", "/squink/squink-0.1.0-SNAPSHOT-standalone.jar"]

EXPOSE 80
