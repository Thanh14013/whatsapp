# WhatsApp Clone - CV Project Summary

## Description

Designed and implemented a production-ready, enterprise-grade real-time messaging platform built with microservices architecture and Domain-Driven Design principles. The system provides comprehensive instant messaging capabilities including one-to-one real-time chat via WebSocket with automatic message delivery status tracking (sent → delivered → read receipts), enabling users to know exactly when their messages reach recipients. Features robust offline message handling where messages are queued and delivered when users reconnect, complemented by push notification integration through Firebase Cloud Messaging for Android/Web and Apple Push Notification service for iOS to alert offline users instantly. Supports multi-device connectivity allowing users to seamlessly receive messages across multiple devices (phone, tablet, web) with synchronized message history and conversation state. Implements secure user authentication and authorization using JWT tokens with BCrypt password hashing, along with user profile management, online/offline presence status tracking, and contact management. The chat service provides conversation management with message history retrieval using cursor-based pagination, message reply functionality, and configurable message retention policies with automatic cleanup jobs. Includes comprehensive security features such as role-based access control, API rate limiting to prevent abuse, account lockout mechanisms after failed login attempts, and TLS encryption for data in transit. The platform is built on a distributed architecture with 6 independent microservices (API Gateway, User Service, Chat Service, Message Processor, Notification Service, Scheduled Jobs) communicating through event-driven patterns using RabbitMQ message queues for decoupled, resilient inter-service communication. Employs a polyglot persistence strategy with PostgreSQL for transactional user and conversation metadata, MongoDB for scalable message content storage with TTL indexes for automatic expiration, and Redis for high-performance caching and session management. The entire infrastructure is containerized with Docker and orchestrated using Docker Compose, featuring production-ready configurations including health checks, circuit breakers for fault tolerance, graceful shutdown handling, and comprehensive observability through Prometheus metrics, Grafana dashboards, and ELK stack for centralized structured logging.

## Tech Stack

Java
Spring Boot
Spring Cloud Gateway
Spring Data JPA
Spring Data MongoDB
Spring Data Redis
Spring AMQP
Spring WebSocket
Spring Security
Spring Actuator
PostgreSQL
MongoDB
Redis
RabbitMQ
Firebase Admin SDK
Docker
Docker Compose
Nginx
Prometheus
Grafana
Elasticsearch
Logstash
Kibana
Flyway
MapStruct
Lombok
Bucket4j
Resilience4j
JJWT
Micrometer
Jackson
Jakarta Validation
HikariCP
Testcontainers
JUnit
Mockito
REST Assured
Maven

## Skills

Microservices Architecture
Domain-Driven Design
Hexagonal Architecture
Event-Driven Architecture
CQRS
API Gateway Pattern
Dual Database Pattern
Polyglot Persistence
WebSocket Protocol
STOMP Protocol
Real-time Communication
Message Queue Integration
Asynchronous Processing
JWT Authentication
BCrypt Password Hashing
Role-Based Access Control
Rate Limiting
Circuit Breaker Pattern
Retry Pattern
Token Bucket Algorithm
Database Sharding
Database Partitioning
Read Replica Strategy
Connection Pooling
Index Optimization
TTL Indexes
Compound Indexes
Cache-Aside Pattern
Write-Through Caching
Multi-Level Caching
Session Management
Push Notification
Multi-Device Support
Snowflake ID Generation
Aggregate Root Pattern
Repository Pattern
Factory Method Pattern
Value Object Pattern
Builder Pattern
Strategy Pattern
Horizontal Scaling
Sticky Sessions
Redis Pub/Sub
Database Migration
Batch Operations
Soft Delete Pattern
Graceful Shutdown
Health Checks
Metrics Collection
Structured Logging
Distributed Tracing
Container Orchestration
Reverse Proxy Configuration
Load Balancing
TLS Termination
CORS Configuration
API Documentation
RESTful API Design
Pagination
DTO Mapping
Bean Validation
Exception Handling
Unit Testing
Integration Testing
Test-Driven Development

## Tools

IntelliJ IDEA
Maven
Git
Docker Desktop
Postman
Apache Bench
Firebase Console
RabbitMQ Management Console
pgAdmin
MongoDB Compass
Redis CLI
Kibana
Grafana
Prometheus
Visual Studio Code
