plugins {
    java
    application
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "chat"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

//    // Логирование
//    implementation("org.slf4j:slf4j-api:2.0.13")
//    implementation("ch.qos.logback:logback-classic:1.5.6")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // Тесты
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

application {
    mainClass.set("chat.moviechat.ChatApplication")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "chat.moviechat.ChatApplication"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}