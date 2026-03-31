# Architecture

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Domain Model](#2-domain-model)
3. [Database Schema](#3-database-schema)
4. [Reconciliation Flow](#4-reconciliation-flow)
5. [State Machines](#5-state-machines)

---

## 1. System Overview

High-level view of the application layers and their dependencies.

```mermaid
graph TD
    Client(["Client (HTTP)"])
    Scheduler["ReconciliationScheduler\n@Scheduled every 15 min"]

    subgraph Controller ["Controller Layer"]
        RC["ReconciliationController\nPOST /v1/reconciliations/trigger\nGET  /v1/reconciliations/report"]
        GEH["GlobalExceptionHandler"]
        Mapper["ReconciliationResultMapper"]
    end

    subgraph Application ["Application Layer"]
        RAS["ReconciliationApplicationService\nrunReconciliation()\ngetReport(...)"]
    end

    subgraph Domain ["Domain Layer"]
        RS["ReconciliationService\nreconcile(order, payment)"]
        Order["Order"]
        Payment["Payment"]
        RR["ReconciliationResult"]
        Money["Money (Value Object)"]
    end

    subgraph Persistence ["Persistence Layer"]
        OR["OrderRepository"]
        PR["PaymentRepository"]
        RRR["ReconciliationResultRepository"]
    end

    DB[("PostgreSQL\nfinsync_monolith")]

    Client -->|HTTP| RC
    RC --> RAS
    RC --> Mapper
    Scheduler --> RAS
    RAS --> RS
    RAS --> OR
    RAS --> PR
    RAS --> RRR
    RS --> Order
    RS --> Payment
    RS --> RR
    Order --> Money
    Payment --> Money
    OR --> DB
    PR --> DB
    RRR --> DB
```

---

## 2. Domain Model

Class diagram showing all domain aggregates, value objects, enums, and their relationships.

```mermaid
classDiagram
    class Money {
        <<Embeddable>>
        +BigDecimal value
        +String currency
        +Money(BigDecimal value, String currency)
        +multiply(int quantity) Money
        +add(Money other) Money
    }

    class OrderItem {
        +UUID id
        +String description
        +Integer quantity
        +Money unitPrice
        +of(Order, String, Integer, Money) OrderItem$
        +subtotal() Money
    }

    class Order {
        +UUID id
        +String externalReference
        +Money amount
        +OrderStatus status
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
        +place(String, List~OrderItem~) Order$
        +pay() void
        +cancel() void
        +calculateTotal() Money
    }

    class OrderStatus {
        <<Enumeration>>
        PENDING
        PAID
        CANCELLED
    }

    class Payment {
        +UUID id
        +String gatewayPaymentId
        +String idempotencyKey
        +String orderExternalReference
        +Money amount
        +PaymentMethod method
        +PaymentStatus status
        +LocalDateTime receivedAt
        +LocalDateTime processedAt
        +receive(String, String, String, Money, PaymentMethod) Payment$
        +markProcessed() void
        +markFailed() void
        +isEligibleForReconciliation() boolean
    }

    class PaymentStatus {
        <<Enumeration>>
        RECEIVED
        PROCESSED
        FAILED
    }

    class PaymentMethod {
        <<Enumeration>>
        PIX
        BOLETO
        CARD
    }

    class ReconciliationResult {
        +UUID id
        +Order order
        +Payment payment
        +ReconciliationStatus status
        +String divergenceReason
        +LocalDateTime reconciledAt
        +matched(Order, Payment) ReconciliationResult$
        +divergent(Order, Payment, String) ReconciliationResult$
        +suspiciousPayment(Payment) ReconciliationResult$
    }

    class ReconciliationStatus {
        <<Enumeration>>
        MATCHED
        DIVERGED
        UNMATCHED
    }

    class ReconciliationService {
        <<DomainService>>
        +reconcile(Order, Payment) ReconciliationResult
    }

    Order "1" --> "many" OrderItem : contains
    Order --> Money : amount
    Order --> OrderStatus : status
    OrderItem --> Money : unitPrice

    Payment --> Money : amount
    Payment --> PaymentStatus : status
    Payment --> PaymentMethod : method

    ReconciliationResult --> Order : order (optional)
    ReconciliationResult --> Payment : payment
    ReconciliationResult --> ReconciliationStatus : status

    ReconciliationService ..> Order : reads
    ReconciliationService ..> Payment : reads
    ReconciliationService ..> ReconciliationResult : creates
```

---

## 3. Database Schema

Entity-relationship diagram showing all tables and constraints.

```mermaid
erDiagram
    orders {
        UUID id PK
        VARCHAR external_reference UK
        NUMERIC amount
        VARCHAR currency
        VARCHAR status
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    order_items {
        UUID id PK
        UUID order_id FK
        VARCHAR description
        INT quantity
        NUMERIC amount
        VARCHAR currency
    }

    payments {
        UUID id PK
        VARCHAR gateway_payment_id
        VARCHAR idempotency_key UK
        VARCHAR order_external_reference
        NUMERIC amount
        VARCHAR currency
        VARCHAR method
        VARCHAR status
        TIMESTAMP received_at
        TIMESTAMP processed_at
    }

    reconciliation_results {
        UUID id PK
        UUID order_id FK
        UUID payment_id FK
        VARCHAR status
        TEXT divergence_reason
        TIMESTAMP reconciled_at
    }

    orders ||--o{ order_items : "has"
    orders ||--o{ reconciliation_results : "referenced by"
    payments ||--o{ reconciliation_results : "referenced by"
```

---

## 4. Reconciliation Flow

Sequence diagram of both the scheduled and manual reconciliation triggers.

```mermaid
sequenceDiagram
    participant Client
    participant Scheduler as ReconciliationScheduler
    participant Controller as ReconciliationController
    participant AppService as ReconciliationApplicationService
    participant DomainSvc as ReconciliationService
    participant OrderRepo as OrderRepository
    participant PaymentRepo as PaymentRepository
    participant ResultRepo as ReconciliationResultRepository
    participant DB as PostgreSQL

    alt Manual trigger
        Client->>+Controller: POST /v1/reconciliations/trigger
        Controller->>AppService: runReconciliation()
    else Scheduled trigger
        Scheduler->>AppService: runReconciliation()
    end

    Note over AppService: Phase 1 — Match orders to payments

    AppService->>+OrderRepo: findAllByStatus(PENDING)
    OrderRepo->>DB: SELECT * FROM orders WHERE status = 'PENDING'
    DB-->>OrderRepo: [Order, ...]
    OrderRepo-->>-AppService: List~Order~

    loop For each pending Order
        AppService->>+PaymentRepo: findByOrderExternalReferenceAndStatus(ref, RECEIVED)
        PaymentRepo->>DB: SELECT * FROM payments WHERE order_external_reference = ? AND status = 'RECEIVED'
        DB-->>PaymentRepo: Optional~Payment~
        PaymentRepo-->>-AppService: Optional~Payment~

        alt Payment found
            AppService->>+DomainSvc: reconcile(order, payment)

            alt Amounts and currencies match
                DomainSvc->>DomainSvc: ReconciliationResult.matched(order, payment)
                DomainSvc-->>-AppService: result [MATCHED]
                AppService->>AppService: order.pay()
                AppService->>AppService: payment.markProcessed()
            else Mismatch detected
                DomainSvc->>DomainSvc: ReconciliationResult.divergent(order, payment, reason)
                DomainSvc-->>AppService: result [DIVERGED]
            end

            AppService->>+ResultRepo: save(result)
            ResultRepo->>DB: INSERT INTO reconciliation_results ...
            DB-->>ResultRepo: saved
            ResultRepo-->>-AppService: ok
        end
    end

    Note over AppService: Phase 2 — Flag orphan payments

    AppService->>+PaymentRepo: findReceivedWithoutMatchingOrder()
    PaymentRepo->>DB: SELECT p.* FROM payments p LEFT JOIN orders o ON ... WHERE o.id IS NULL
    DB-->>PaymentRepo: [Payment, ...]
    PaymentRepo-->>-AppService: List~Payment~

    loop For each orphan Payment
        AppService->>AppService: ReconciliationResult.suspiciousPayment(payment)
        AppService->>+ResultRepo: save(result [UNMATCHED])
        ResultRepo->>DB: INSERT INTO reconciliation_results ...
        ResultRepo-->>-AppService: ok
    end

    alt Manual trigger
        Controller-->>-Client: 200 { triggeredAt }
    end
```

---

## 5. State Machines

### Order lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING : Order.place()
    PENDING --> PAID : order.pay()\n[reconciliation MATCHED]
    PENDING --> CANCELLED : order.cancel()
    PAID --> [*]
    CANCELLED --> [*]
```

### Payment lifecycle

```mermaid
stateDiagram-v2
    [*] --> RECEIVED : Payment.receive()
    RECEIVED --> PROCESSED : payment.markProcessed()\n[reconciliation MATCHED]
    RECEIVED --> FAILED : payment.markFailed()
    PROCESSED --> [*]
    FAILED --> [*]
```

### Reconciliation result outcomes

```mermaid
stateDiagram-v2
    [*] --> Reconciling : runReconciliation()

    Reconciling --> MATCHED : amounts and currencies match
    Reconciling --> DIVERGED : amount or currency mismatch
    Reconciling --> UNMATCHED : payment has no corresponding order

    MATCHED --> [*]
    DIVERGED --> [*]
    UNMATCHED --> [*]
```