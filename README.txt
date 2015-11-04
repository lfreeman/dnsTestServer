Start service:
./mvnw spring-boot:run
or
java -jar target/dnsTestServer-0.0.1-SNAPSHOT.jar


ssh dns@localhost -p 2000
http://localhost:8081/health
