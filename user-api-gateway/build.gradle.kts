object Versions {
    const val GRPC = "4.34.1"
    const val GRPC_KOTLIN = "1.4.1"
    const val GRPC_PROTO = "1.80.0"
    const val JWT = "0.12.6"
}

plugins {
    kotlin("jvm")
    id("com.google.protobuf") version "0.9.5"
}

the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.2")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    implementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")

    implementation("com.google.protobuf:protobuf-kotlin:${Versions.GRPC}")
    implementation("io.grpc:grpc-kotlin-stub:${Versions.GRPC_KOTLIN}")

    implementation("io.grpc:grpc-protobuf:${Versions.GRPC_PROTO}")
    implementation("io.grpc:grpc-netty:${Versions.GRPC_PROTO}")
    implementation("io.grpc:grpc-stub:${Versions.GRPC_PROTO}")
    implementation("io.grpc:grpc-netty-shaded:${Versions.GRPC_PROTO}")

    // Spring Cloud Gateway 2025.0.x (Spring Boot 3.5.x 호환)
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("io.jsonwebtoken:jjwt-api:${Versions.JWT}")

    runtimeOnly("io.jsonwebtoken:jjwt-impl:${Versions.JWT}")

    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Versions.JWT}")
}

sourceSets{
    getByName("main"){
        java {
            srcDirs(
                "build/generated/source/proto/main/java",
                "build/generated/source/proto/main/kotlin"
            )
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${Versions.GRPC}"
    }

    plugins {
        register("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.GRPC_PROTO}"
        }
        register("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${Versions.GRPC_KOTLIN}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                register("grpc")
                register("grpckt")
            }
            it.builtins {
                register("kotlin")
            }
        }
    }
}
