# common-lib

> **Shared library for the WhatsApp Clone microservices platform.**  
> Provides standardised DTOs, exception hierarchy, utilities, and auto-configuration that every downstream service can consume as a plain Maven dependency.

---

## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Modules](#modules)
  - [Configuration](#configuration)
  - [Constants](#constants)
  - [DTOs](#dtos)
  - [Exceptions](#exceptions)
  - [Utilities](#utilities)
- [Auto-Configuration](#auto-configuration)
- [Usage in Other Services](#usage-in-other-services)
- [Building](#building)
- [Tech Stack](#tech-stack)

---

## Overview

`common-lib` is a **Spring Boot auto-configuration library** (not an executable application). It is designed to be declared as a `<dependency>` in any microservice that is part of the WhatsApp Clone system. Once on the classpath, Spring Boot automatically applies all common beans and configurations.

```
┌─────────────────────────────────────────────────────┐
│                   WhatsApp Clone                    │
│                                                     │
│  user-service  message-service  notification-service│
│       │               │                │            │
│       └───────────────┴────────────────┘            │
│                       │                             │
│               [ common-lib ]                        │
│  DTOs · Exceptions · Utils · ObjectMapper Config    │
└─────────────────────────────────────────────────────┘
```

---

## Project Structure

```
common-lib/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/whatsapp/
    │   │   ├── common/
    │   │   │   ├── config/
    │   │   │   │   └── CommonConfig.java          # ObjectMapper bean
    │   │   │   ├── constant/
    │   │   │   │   └── ErrorCode.java             # Application-wide error codes
    │   │   │   ├── dto/
    │   │   │   │   ├── BaseResponse.java          # Standard API response wrapper
    │   │   │   │   └── PageResponse.java          # Pagination response wrapper
    │   │   │   ├── exception/
    │   │   │   │   ├── BaseException.java         # Root exception class
    │   │   │   │   ├── BusinessException.java     # 400 – business rule violation
    │   │   │   │   └── ResourceNotFoundException.java  # 404 – resource not found
    │   │   │   └── util/
    │   │   │       ├── DateUtil.java              # Date/time helpers (UTC)
    │   │   │       ├── JsonUtil.java              # JSON serialisation helpers
    │   │   │       └── ValidationUtil.java        # Input validation helpers
    │   │   └── commonlib/
    │   │       └── CommonLibApplication.java      # Auto-configuration entry point
    │   └── resources/
    │       ├── application.yaml
    │       └── META-INF/spring/
    │           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/
        └── java/com/whatsapp/commonlib/
            └── CommonLibApplicationTests.java
```

---

## Getting Started

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ (or use the included `mvnw` wrapper) |

### Install to local Maven repository

```bash
./mvnw clean install
```

---

## Modules

### Configuration

**`com.whatsapp.common.config.CommonConfig`**

Registers a shared `ObjectMapper` bean configured with:

- `JavaTimeModule` — full Java 8 date/time support.
- ISO-8601 date serialisation (no epoch timestamps).
- `FAIL_ON_EMPTY_BEANS` disabled for safe serialisation of empty POJOs.

---

### Constants

**`com.whatsapp.common.constant.ErrorCode`**

Centralised, category-prefixed string constants used as machine-readable error codes in all API responses and exception handling.

| Prefix | Description |
|--------|-------------|
| `VALIDATION_*` | Input / field validation failures |
| `AUTH_*` | Authentication & authorisation errors |
| `USER_*` | User domain errors |
| `MESSAGE_*` | Messaging domain errors |
| `RESOURCE_*` | Generic resource CRUD errors |
| `SYSTEM_*` | Infrastructure / server errors |
| `RATE_*` | Rate limiting violations |

---

### DTOs

#### `BaseResponse<T>`

Standard envelope for **all API responses**.

```json
// Success
{
  "success": true,
  "message": "User created successfully",
  "data": { ... },
  "timestamp": "2026-02-22T08:00:00Z"
}

// Error
{
  "success": false,
  "message": "Validation failed",
  "error": { "code": "VALIDATION_ERROR", "details": "phone is required" },
  "timestamp": "2026-02-22T08:00:00Z"
}
```

Factory methods:

```java
BaseResponse.success(data)
BaseResponse.success("Created", data)
BaseResponse.error("Not found")
BaseResponse.error("Failed", new ErrorDetails("CODE", "detail", null))
```

#### `PageResponse<T>`

Standard envelope for **paginated list endpoints**.

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

Factory method:

```java
PageResponse.of(contentList, page, size, totalElements)
```

---

### Exceptions

All exceptions extend `BaseException` (which extends `RuntimeException`) and carry:
- `message` – human-readable description
- `errorCode` – machine-readable code from `ErrorCode`
- `httpStatus` – the intended HTTP status code
- `metadata` – optional arbitrary payload

| Class | HTTP Status | Default Error Code |
|-------|-------------|-------------------|
| `BaseException` | 500 | `INTERNAL_ERROR` |
| `BusinessException` | 400 | `BUSINESS_ERROR` |
| `ResourceNotFoundException` | 404 | `RESOURCE_NOT_FOUND` |

**Usage examples:**

```java
throw new ResourceNotFoundException("User", userId);

throw new BusinessException("Phone number already registered",
        ErrorCode.PHONE_TAKEN);
```

---

### Utilities

#### `DateUtil`

Static helpers for UTC-based date/time operations:

```java
DateUtil.now()                          // Instant (UTC)
DateUtil.today()                        // LocalDate (UTC)
DateUtil.formatISO(instant)             // "2026-02-22T08:00:00Z"
DateUtil.parseISO(string)               // Instant
DateUtil.daysBetween(start, end)
DateUtil.addDays(instant, 7)
DateUtil.startOfDay(date)
DateUtil.isSameDay(instant1, instant2)
```

#### `JsonUtil`

Static helpers wrapping `ObjectMapper`:

```java
JsonUtil.toJson(object)                 // String
JsonUtil.toPrettyJson(object)           // indented String
JsonUtil.fromJson(json, MyDto.class)    // MyDto
JsonUtil.fromJson(json, new TypeReference<List<MyDto>>(){})
JsonUtil.convert(source, TargetDto.class)
JsonUtil.getObjectMapper()              // ObjectMapper instance
```

#### `ValidationUtil`

Static helpers for input validation (backed by Apache Commons Lang3):

```java
ValidationUtil.isBlank(str)
ValidationUtil.isValidEmail("user@example.com")   // true/false
ValidationUtil.isValidPhoneNumber("+84912345678") // E.164
ValidationUtil.isValidUsername("john_doe")
ValidationUtil.isLengthValid(str, 3, 30)
ValidationUtil.requireNonBlank(str, "Name required")
ValidationUtil.requireValidEmail(email)
```

---

## Auto-Configuration

`common-lib` uses **Spring Boot 3 auto-configuration** (`@AutoConfiguration` + `AutoConfiguration.imports`).

When added as a dependency, consumers do **not** need any `@Import` or `@ComponentScan` annotation — Spring Boot discovers and applies `CommonLibApplication` automatically via:

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## Usage in Other Services

1. Install `common-lib` to your local Maven repository:

   ```bash
   ./mvnw clean install
   ```

2. Add the dependency to your service's `pom.xml`:

   ```xml
   <dependency>
       <groupId>com.whatsapp</groupId>
       <artifactId>common-lib</artifactId>
       <version>1.0.0-SNAPSHOT</version>
   </dependency>
   ```

3. Use the shared classes directly — no extra configuration needed:

   ```java
   // In a @RestController
   return ResponseEntity.ok(BaseResponse.success("Done", result));

   // In a @Service
   throw new ResourceNotFoundException("Message", messageId);
   ```

---

## Building

```bash
# Compile + test + package
./mvnw clean install

# Skip tests
./mvnw clean install -DskipTests

# Run tests only
./mvnw test
```

---

## Tech Stack

| Library | Version | Purpose |
|---------|---------|---------|
| Spring Boot | 3.2.3 | Auto-configuration framework |
| Spring Boot Web | 3.2.3 | Web MVC support for DTOs |
| Spring Boot Validation | 3.2.3 | Bean Validation (JSR-380) |
| Jackson Databind | (BOM) | JSON serialisation |
| Jackson Datatype JSR310 | (BOM) | Java 8 date/time support |
| Lombok | 1.18.30 | Boilerplate reduction |
| Apache Commons Lang3 | (BOM) | String / reflection utilities |
| Apache Commons Collections4 | 4.4 | Collection utilities |
| SLF4J | (BOM) | Logging facade |
| JUnit 5 | (BOM) | Unit & integration testing |

---

> © WhatsApp Clone Team — for educational / system design purposes only.

