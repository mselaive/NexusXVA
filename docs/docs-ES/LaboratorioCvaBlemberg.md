# Laboratorio CVA con Blemberg

Este escenario explica con un caso visible cómo se conectan portfolio, counterparty, netting, collateral y curvas.

## 1. Cargar el escenario

Con NexusXVA y PostgreSQL corriendo:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_xva_credit_lab.sql
```

El script es repetible y crea:

- Portfolio: `XVA Demo - Prime Broker Credit Lab`.
- 12 opciones confirmadas sobre AAPL, MSFT, SPY, QQQ, JPM y GLD.
- Counterparty: `Demo Prime Broker A`, rating `A`.
- Netting set: `Demo Prime Broker A - USD Netting`.
- Collateral inicial: USD 0, para que el primer CVA muestre exposición y contribuciones visibles.

No crea curvas. Esa parte debe hacerse desde Blemberg para conservar la trazabilidad real del dato.

## 2. Qué representa cada objeto

| Objeto | Pregunta que responde |
|---|---|
| Portfolio | ¿Qué posiciones posee NexusXVA? |
| Counterparty | ¿Con quién existe el riesgo de crédito? |
| Netting set | ¿Qué portfolios se compensan bajo el mismo acuerdo? |
| Collateral | ¿Qué parte de la exposición está cubierta? |
| Credit curve | ¿Cómo crece la probabilidad de default en el tiempo? |
| Discount curve | ¿Cuánto vale hoy una exposición futura? |

La counterparty no contiene trades. El netting set conecta el portfolio con esa counterparty.

## 3. Importar la credit curve

1. Iniciar Blemberg y refrescar `CREDIT_SPREADS`.
2. Entrar a NexusXVA como ADMIN.
3. Abrir **XVA Setup**.
4. Elegir `Demo Prime Broker A`.
5. En **Credit curves**, usar **Import market draft**.
6. Elegir la fecha de valoración y recovery rate `40%`.
7. Mantener `Allow stale` apagado y ejecutar el import.

Blemberg usa el rating `A` para entregar un proxy OAS agregado. NexusXVA transforma y guarda la respuesta como una curva `DRAFT`, inactiva y de origen `MARKET_DATA`.

Revisar antes de aprobar:

- rating bucket `A`;
- OAS observado;
- hazard proxy;
- recovery rate;
- serie y fecha de observación;
- siete probabilidades acumuladas crecientes;
- badge `Market proxy` y estado stale.

Después usar **Approve**. Solo una curva aprobada y activa puede alimentar CVA.

## 4. Importar la discount curve

En **Discount curves**, usar el import de market data para USD, revisar sus factores y aprobar el draft. Esta curva representa descuento temporal; no representa riesgo de crédito de la counterparty.

## 5. Ejecutar CVA

1. Cambiar al grupo FO.
2. Abrir **CVA**.
3. Elegir scope **Netting set**.
4. Seleccionar `Demo Prime Broker A - USD Netting`.
5. Usar curve mode.
6. Seleccionar la credit curve A aprobada y la discount curve USD aprobada.
7. Usar LGD `0.60`, coherente con recovery `0.40`.
8. Ejecutar CVA.

La fecha final requerida es `valuationDate + horizonDays`. Ambas curvas deben tener un último punto en esa fecha o después. Para el tramo entre la fecha de valoración y el primer punto, NexusXVA usa las anclas financieras naturales `survivalProbability = 1.0` y `discountFactor = 1.0` e interpola; nunca extrapola después del último punto.

El cálculo simplificado agrega por bucket:

```text
CVA contribution = discounted exposure x default probability increment x LGD
```

El CVA total es la suma de esas contribuciones. El collateral del netting set reduce la exposición positiva antes de aplicar default y descuento.

## 6. Pruebas para entender el resultado

Ejecutar una vez con collateral USD 0. Después, como ADMIN:

1. Cambiar collateral a USD 250,000 y repetir CVA: debería disminuir o llegar a cero.
2. Restaurar collateral a `0` para continuar usando el laboratorio.
3. Crear o importar una curva de peor crédito, por ejemplo `BBB`, para una counterparty BBB equivalente: el CVA debería aumentar si la exposición no cambia.
4. No aprobar un draft y comprobar que no aparece como curva operable.

Este escenario demuestra el contrato actual. La credit curve es un proxy de mercado por rating, no una curva CDS específica de `Demo Prime Broker A`.
