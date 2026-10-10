plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.northwind"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// One property controls every Embabel artifact: never mix versions
val embabelVersion = "1.5.3"

// Choose a model provider at build time: ./gradlew bootRun -Pprovider=openai (Ollama is the default)
val provider = (findProperty("provider") as String?) ?: "ollama"

dependencies {
    implementation("com.embabel.agent:embabel-agent-starter:$embabelVersion")
    implementation("com.embabel.agent:embabel-agent-starter-$provider:$embabelVersion")

    testImplementation("com.embabel.agent:embabel-agent-test:$embabelVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
