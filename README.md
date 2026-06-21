<p align="center">
  <img src="docs/assets/nexusxva-logo.svg" alt="NexusXVA logo" width="132" />
</p>

# NexusXVA

NexusXVA es una workstation de riesgo para aprender y construir flujos tipo Front Office, Back Office y XVA sobre portfolios de opciones europeas.

El proyecto no intenta ser Murex ni Bloomberg. La idea es construir, paso a paso, una plataforma clara donde se vea el ciclo completo:

```text
FO analiza y bookea
  -> BO valida
  -> portfolio confirmado
  -> pricing
  -> exposure
  -> CVA
  -> dashboard operativo
```

## Estado Actual

- Backend Java Spring Boot con PostgreSQL, Flyway, JPA y Testcontainers.
- Frontend Next.js con pantallas por grupo activo.
- Auth con usuarios multi-grupo: `FO`, `BO`, `ADMIN`.
- Portfolio management persistido.
- u-Pad para enviar bookings a BO.
- Amendments y cancellations con maker-checker.
- Notificaciones persistidas por usuario.
- Black-Scholes pricing individual y portfolio-level.
- Monte Carlo Exposure V1.
- CVA V1.1 con modo flat y curvas simples.
- Integracion market data via frontera `marketdata`, con Blemberg o provider local.

## Arquitectura

```mermaid
flowchart LR
    UI[Next.js Dashboard] --> API[NexusXVA Backend]
    API --> Auth[Auth + Groups]
    API --> Portfolio[Portfolio Store]
    API --> Pricing[Pricing Domain]
    API --> Exposure[Exposure / Monte Carlo]
    API --> CVA[CVA]
    API --> MarketData[Marketdata Port]
    MarketData --> Blemberg[Blemberg Service]
    Portfolio --> DB[(PostgreSQL)]
    Auth --> DB
    API --> Notifications[Notifications]
    Notifications --> DB
```

## Workflow Operativo

```mermaid
flowchart TD
    A[FO Pre-Trade Analysis] --> B[u-Pad Booking]
    B --> C[PENDING_VALIDATION]
    C --> D{BO Trade Validation}
    D -->|Approve| E[Confirmed ACTIVE Position]
    D -->|Reject| F[Rejected Booking]
    E --> G[Pricing / Exposure / CVA]
    E --> H[FO Amend or Cancel Request]
    H --> I{BO Lifecycle Review}
    I -->|Approve Cancel| J[CANCELLED History]
    I -->|Approve Amend| K[AMENDED History + New ACTIVE Position]
    I -->|Reject| E
```

## Grupos

- **FO**: FO Desk, Pre-Trade Analysis, Stress Testing, u-Pad, Portfolios, Pricing, Exposure y CVA.
- **BO**: Trade Validation, Lifecycle Validation y Trading Limits.
- **ADMIN**: usuarios, grupos, permisos FO, visibilidad de portfolios y workflow map.

Un usuario puede tener varios grupos. Al hacer login elige el grupo activo de la sesion.

## Posiciones Y Lifecycle

Las posiciones confirmadas tienen `lifecycleStatus`:

- `ACTIVE`: entra en pricing, exposure, stress y CVA.
- `CANCELLED`: historica, no entra en analytics.
- `AMENDED`: historica, no entra en analytics.

Cuando BO aprueba un amendment, la posicion original queda `AMENDED` y se crea una nueva posicion `ACTIVE`. Por eso no se vuelve a modificar la posicion `AMENDED`; el siguiente cambio debe hacerse sobre la nueva posicion activa.

## Notificaciones

NexusXVA guarda notificaciones persistidas por usuario:

- BO recibe aviso cuando FO envia un booking, amendment o cancellation.
- FO recibe aviso cuando BO aprueba o rechaza sus solicitudes.
- La campana del header muestra unread count y permite marcar notificaciones como leidas.

## Correr Todo

```bash
docker compose up --build
```

URLs habituales:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Blemberg externo, si esta levantado: `http://localhost:8081`

## Documentacion

- Backend: [backend/README.md](backend/README.md)
- Logica del sistema EN: [docs/docs-EN/SystemLogic.md](docs/docs-EN/SystemLogic.md)
- Logica del sistema ES: [docs/docs-ES/LogicaDelSistema.md](docs/docs-ES/LogicaDelSistema.md)
- Conceptos financieros EN: [docs/docs-EN/FinancialConcepts.md](docs/docs-EN/FinancialConcepts.md)
- Conceptos financieros ES: [docs/docs-ES/ConceptosFinancieros.md](docs/docs-ES/ConceptosFinancieros.md)

## Siguiente Camino

Los siguientes candidatos naturales son:

- Persisted valuation run history.
- Mejor reporting FO/BO sobre lifecycle.
- UI para CVA curve mode.
- Counterparties, netting y collateral.
- FX y multi-currency.
