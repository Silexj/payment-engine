
# High-Performance Payment Processing Engine

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-green)
![Postgres](https://img.shields.io/badge/PostgreSQL-15-blue)
![Kafka](https://img.shields.io/badge/Apache_Kafka-KRaft-red)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)

Отказоустойчивый движок финансовых транзакций. Реализует строгую консистентность данных, идемпотентность и асинхронную событийную модель.

##  Ключевые особенности

*   **Money Transfer Core:** Реализация переводов с защитой от **Race Conditions** (Pessimistic Locking) и **Deadlocks** (Resource ordering).
*   **Idempotency:** Гарантия защиты от дублирующих списаний при сетевых сбоях.
*   **Transactional Outbox:** Гарантированная доставка событий в Kafka без использования распределенных транзакций.
*   **Audit Trail:** Полная история всех движений средств.
*   **Event-Driven:** Асинхронные уведомления через Kafka.
*   **Observability:** Встроенный мониторинг метрик производительности и бизнес-показателей.

##  Технический стек

*   **Language:** Java 21
*   **Framework:** Spring Boot 3 (Data JPA, Web, Kafka)
*   **Database:** PostgreSQL 15 (Liquibase/Flyway migrations)
*   **Messaging:** Apache Kafka (KRaft)
*   **Observability:** Prometheus, Grafana, Micrometer
*   **Testing:** JUnit 5, Testcontainers (Real DB & Kafka integration tests)
*   **Ops:** Docker, Docker Compose

##  Архитектура

### Процесс перевода
1.  **API:** Клиент отправляет POST `/transfers` с `Idempotency-Key`.
2.  **Locking:** Сервис сортирует ID счетов и захватывает пессимистичные блокировки (`SELECT FOR UPDATE`).
3.  **Logic:** Проверка баланса -> Обновление балансов -> Запись в `transactions` -> Запись в `outbox_events`. Всё в одной ACID транзакции.
4.  **Async:** Scheduler вычитывает события из `outbox_events` и пушит в Kafka.
5.  **Notify:** Consumer читает Kafka и отправляет уведомление.


## Monitoring & Observability

Проект предоставляет готовые дашборды для мониторинга в реальном времени.

### Доступ:

*    **Grafana:** http://localhost:3000 (Login: admin / Pass: admin)

*    **Prometheus:** http://localhost:9090
## Запуск

Требуется Docker и Java 21.

1.    **Поднятие инфраструктуры (App + DB + Kafka + Monitoring):**
```bash
docker-compose up -d --build
```
2.    **Проверка статуса:**
Приложение доступно по адресу http://localhost:8080.

## Тестирование

Проект покрыт интеграционными тестами с использованием **Testcontainers**.

```bash
./gradlew test
```
  

## API Examples

### 1. Создать счет

```bash  
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"currency": "RUB"}'
```
  

### 2. Пополнить баланс


```bash    
curl -X POST http://localhost:8080/api/v1/accounts/1/top-up \
  -H "Content-Type: application/json" \
  -d '{"accountId": 1, "amount": 1000.00}'
```
  

### 3. Перевести деньги

```bash    
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "externalId": "550e8400-e29b-41d4-a716-446655440000",
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 500.00
  }'
```
