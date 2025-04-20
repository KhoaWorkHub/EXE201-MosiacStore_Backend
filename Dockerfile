# Build stage
FROM amazoncorretto:17-alpine AS build
WORKDIR /workspace/app

# Sao chép Maven wrapper và pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Cấp quyền thực thi cho mvnw
RUN chmod +x ./mvnw

# Tải dependencies để tận dụng cache Docker layer
RUN ./mvnw dependency:go-offline -B

# Sao chép source code
COPY src src

# Build ứng dụng
RUN ./mvnw package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# Runtime stage
FROM amazoncorretto:17-alpine
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency

# Sao chép các thành phần build từ stage trước
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# Cấu hình biến môi trường
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java","-cp","app:app/lib/*","com.mosiacstore.mosiac.MosiacApplication"]