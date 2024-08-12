plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
}

group = "com.si.main"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

configurations {
    all {
        exclude(group = "commons-logging", module = "commons-logging")
    }
}

dependencies {
    implementation("com.google.api-client:google-api-client:1.32.1")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.32.1")
    implementation("com.google.apis:google-api-services-searchconsole:v1-rev20211026-1.32.1")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.google.http-client:google-http-client-gson:1.41.5")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.4.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.apache.poi:poi-ooxml:5.2.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    compileOnly ("org.projectlombok:lombok")
    annotationProcessor ("org.projectlombok:lombok")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.analytics:google-analytics-data:0.16.0")
}


kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
