FROM java:8
COPY target/swingit-0.0.1-SNAPSHOT.jar /
WORKDIR /
EXPOSE 8070
CMD ["java", "-cp",  "swingit-0.0.1-SNAPSHOT.jar",  "se.babtist.swingit.SwingitService"]
