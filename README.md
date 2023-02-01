# E-Commerce Microservice Platform

A microservice-based e-commerce platform built with **Spring Boot 2.4** and **React 17**.

## Architecture

The platform consists of the following microservices:

- **API Gateway** (Port 8080) - Spring Cloud Gateway for request routing
- **Product Service** (Port 8081) - Product catalog management
- **Order Service** (Port 8082) - Order processing and management
- **Auth Service** (Port 8083) - Authentication and JWT token management
- **Frontend** (Port 3000) - React 17 single-page application

## Tech Stack

### Backend
- Java 11
- Spring Boot 2.4.5
- Spring Cloud Gateway 2020.0.2
- Spring Security + JWT
- Spring Data JPA
- MySQL 8.0
- Maven

### Frontend
- React 17
- React Router 5
- Axios
- Bootstrap 4.6

## Getting Started

### Prerequisites

- Java 11+
- Maven 3.6+
- MySQL 8.0
- Node.js 14+

### Database Setup

```sql
CREATE DATABASE ecommerce_products;
CREATE DATABASE ecommerce_orders;
CREATE DATABASE ecommerce_auth;
```

### Running the Services

Start each service in order:

```bash
# Auth Service
cd auth-service && mvn spring-boot:run

# Product Service
cd product-service && mvn spring-boot:run

# Order Service
cd order-service && mvn spring-boot:run

# API Gateway
cd api-gateway && mvn spring-boot:run

# Frontend
cd frontend && npm install && npm start
```

## API Endpoints

All requests go through the API Gateway on port 8080:

| Service  | Endpoint               | Description        |
|----------|------------------------|--------------------|
| Auth     | POST /api/auth/login   | User login         |
| Auth     | POST /api/auth/register| User registration  |
| Products | GET /api/products      | List products      |
| Products | GET /api/products/{id} | Get product detail |
| Orders   | POST /api/orders       | Create order       |
| Orders   | GET /api/orders/{id}   | Get order details  |

## License

This project is for educational purposes.
