plugins {
	java
	id("org.springframework.boot") version "4.0.4"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.unishare"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-aspectj")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("com.github.ben-manes.caffeine:caffeine")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	// Cloudinary - File Storage
	implementation("com.cloudinary:cloudinary-http44:1.39.0")

	// File Upload support
	implementation("commons-io:commons-io:2.15.1")

	// Google Calendar API — tạo Google Meet cho buổi mentoring
	implementation("com.google.api-client:google-api-client:2.7.2")
	implementation("com.google.apis:google-api-services-calendar:v3-rev20250404-2.0.0")
	implementation("com.google.auth:google-auth-library-oauth2-http:1.33.1")
	implementation("com.google.oauth-client:google-oauth-client:1.36.0")

	// OpenAPI / Swagger UI (Spring Boot 4 → springdoc 3.x)
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.mockito:mockito-core")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<JavaCompile>("compileTestJava") {
}

tasks.withType<Test> {
	useJUnitPlatform()
	jvmArgs("-Duser.timezone=UTC")
	systemProperty("spring.profiles.active", "test")
}
