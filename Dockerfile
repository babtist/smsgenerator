FROM java:8
COPY target/smsgenerator-0.0.2-SNAPSHOT.jar /
WORKDIR /
EXPOSE 8070
CMD ["java", "-cp",  "smsgenerator-0.0.2-SNAPSHOT.jar",  "se.symsoft.codecamp.SmsGeneratorService"]
