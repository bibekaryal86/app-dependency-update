FROM eclipse-temurin:17-jre-alpine
RUN adduser --system --group springdocker
USER springdocker:springdocker
ARG JAR_FILE=app/build/libs/nospring-service-skeleton.jar
COPY ${JAR_FILE} nospring-service-skeleton.jar
ENTRYPOINT ["java","-jar", \
#"-DPORT=8080", \
#"-DSPRING_PROFILES_ACTIVE=docker", \
#"-DTZ=America/Denver", \
#"-DVAR1=some_var", \
#"-DVAR2=another_var", \
"/nospring-service-skeleton.jar"]
# Environment variables to be prdvided in docker-compose
