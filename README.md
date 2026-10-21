# Core Service (`coreservice`)

The foundational multi-module library framework providing shared security, token validation, distributed tracing, and messaging components across KVS microservices.

---

## 📦 Architecture & Modules

The repository is structured as a Gradle multi-module project where each module produces and publishes its own independent library JAR.

```
core-framework/
├── build.gradle             # Root multi-module build orchestration
├── settings.gradle          # Root settings: includes ms-core & ms-kafka
│
├── ms-core/                 # Core Framework Submodule
│   ├── build.gradle         # Web, Security, JWT, and Tracing dependencies
│   └── src/
│       ├── main/java/com/kvs/core/
│       │   ├── config/      # Filters & security configurations
│       │   └── service/     # Token generation & validation services
│       └── test/java/com/kvs/core/
│
└── ms-kafka/                # Kafka Messaging Submodule
    ├── build.gradle         # Spring Kafka dependencies & messaging configuration
    └── src/
        ├── main/java/com/kvs/core/kafka/
        └── test/java/com/kvs/core/kafka/
```

---

## 🧩 Modules Overview

### 1. `ms-core` (`com.kvs.core:ms-core`)

Contains core enterprise cross-cutting concerns segregated cleanly by functional sub-package:

#### 🔐 Security (`com.kvs.core.config.security`)
* **`JwtAuthenticationFilter`**:
  * Decoupled filter parsing and validating `Bearer` JWT tokens from the `Authorization` header.
  * Resolves authorities from JWT claims (`roles`, `role`, `authorities`).
  * Plugs into Spring Security's standard `UserDetailsService` (if declared in consuming service) or resolves directly from claims.
* **`JwtAuthenticationEntryPoint`**:
  * Authentication entry point that formats filter-level 401 Unauthorized rejections into standard `ApiErrorResponse` JSON.
* **`SecurityConfig`**:
  * Configures global password encryption (`BCryptPasswordEncoder`).

#### 📡 Distributed Tracing & Observation (`com.kvs.core.config.tracing`)
* **`TraceCorrelationFilter`**:
  * Extracts custom tracing headers (`X-Trace-Id`, `X-Span-Id`, `X-Correlation-Id`, `traceId`, `spanId`).
  * Integrates with Brave & Micrometer Tracing.
  * Ensures custom trace IDs are preserved in SLF4J MDC across Spring Security observation scopes (`ObservationFilterChainDecorator`).
  * Echoes trace headers back in HTTP response headers.
* **`TracePropagationClientInterceptor`**:
  * Interceptor automatically propagating `X-Trace-Id` and `X-Span-Id` on outgoing HTTP client requests (`RestTemplate`, `RestClient`).

#### 📝 Auditing (`com.kvs.core.config.audit`)
* **`SpringSecurityAuditorAware`**:
  * Injects current authenticated user email into JPA auditing fields (`@CreatedBy`, `@LastModifiedBy`).

#### 🌐 Web MVC (`com.kvs.core.config.web`)
* **`JwtTokenInterceptor`** & **`WebConfig`**:
  * Optional HandlerInterceptor & WebMvcConfigurer for non-Spring Security interceptor setups.

#### ⚙️ Autoconfiguration (`com.kvs.core.config`)
* **`CoreCommonAutoConfiguration`**:
  * Declared in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  * Conditionally registers `GlobalExceptionHandler`, `JwtAuthenticationEntryPoint`, `TracePropagationClientInterceptor`, and `SpringSecurityAuditorAware` with `@ConditionalOnMissingBean` fallback support.

#### 📦 Envelopes & Domain Exceptions (`com.kvs.core.dto` & `com.kvs.core.exception`)
* **`ApiResponse<T>` & `ApiErrorResponse`**:
  * Standardized success and failure JSON envelopes with synchronized `traceId` and field validation reporting.
* **`GlobalExceptionHandler`**:
  * Centralized `@RestControllerAdvice` formatting domain exceptions (`BaseApiException`, `ResourceNotFoundException`, `ConflictException`, etc.) into `ApiErrorResponse`.

### 2. `ms-kafka` (`com.kvs.core:ms-kafka`)

Contains the shared Apache Kafka messaging infrastructure, topic definitions, and cross-cutting event correlation components.

* **`BaseEvent<T>`**:
  * Standard JSON event envelope (`eventId`, `eventType`, `sourceService`, `timestamp`, `traceId`, `payload`).
* **`KafkaTraceProducerInterceptor`**:
  * Kafka producer interceptor automatically injecting `X-Trace-Id` and `X-Span-Id` from MDC into record headers.
* **`KafkaConstants`**:
  * Standardized header definitions (`X-Trace-Id`, `X-Span-Id`) for distributed trace propagation across Kafka event streams.
* **Spring Kafka Support**:
  * Pre-configured dependencies on `spring-kafka` and `jackson-databind`.

---

## 🚀 Using in Downstream Services

### 1. Add Repositories

Configure `mavenLocal()` and the GitHub Packages repository in your service's `build.gradle`:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/nirajjaysw/nextgenschoolerp")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

### 2. Add Dependencies

Reference the desired submodules independently:

```groovy
dependencies {
    // Core security, JWT & tracing
    implementation 'com.kvs.core:ms-core:0.0.1-SNAPSHOT'

    // Kafka messaging foundation (optional)
    implementation 'com.kvs.core:ms-kafka:0.0.1-SNAPSHOT'
}
```

### 3. Component Scanning

Ensure the consuming Spring Boot application scans the `com.kvs` base package:

```java
@SpringBootApplication
@ComponentScan(basePackages = { "com.kvs" })
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## 🛠️ Build & Publishing Commands

Run the following commands from the root `coreservice` folder:

| Task | Command | Description |
| :--- | :--- | :--- |
| **Compile & Test** | `.\gradlew.bat test` | Compiles sources and runs test suites across all submodules. |
| **Clean Build** | `.\gradlew.bat clean build` | Cleans previous artifacts, runs tests, and generates library JARs. |
| **Publish to Local** | `.\gradlew.bat publishToMavenLocal` | Publishes `ms-core` and `ms-kafka` JARs, POMs, and module metadata to `~/.m2/repository/com/kvs/core/`. |
| **Publish to GitHub Packages** | `.\gradlew.bat publish` | Publishes publications to the GitHub Packages Maven repository. |