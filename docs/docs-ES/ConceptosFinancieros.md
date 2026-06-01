# Guía simple de conceptos financieros para pricing de opciones europeas

## 1. Objetivo del cálculo

El objetivo de este módulo es calcular el **precio teórico de una opción europea** usando el modelo **Black-Scholes**.

Además del precio, también se calculan algunas medidas llamadas **Greeks**, que ayudan a entender cómo cambia el valor de la opción cuando cambian ciertas variables como el precio del activo, la volatilidad o el tiempo.

La idea principal es:

> Dado un conjunto de datos financieros básicos, estimar cuánto debería valer una opción europea y cómo se comporta frente a distintos cambios del mercado.

---

## 2. Qué es una opción financiera

Una opción financiera es un contrato que da a una persona el **derecho**, pero no la obligación, de comprar o vender un activo en el futuro a un precio acordado.

Ese activo puede ser una acción, un índice, una moneda u otro instrumento financiero.

Lo importante es que una opción no obliga a ejecutar la operación. Solo da el derecho a hacerlo si conviene.

---

## 3. Qué es una opción Call

Una **Call** es una opción que da el derecho a **comprar** un activo a un precio fijo en el futuro.

Ejemplo simple:

* Una acción vale hoy 100.
* Tengo una Call que me permite comprarla a 110 en el futuro.

Esta opción se vuelve más valiosa si el precio de la acción sube mucho.

Por ejemplo, si la acción sube a 130, poder comprarla a 110 es conveniente.

En simple:

> Una Call apuesta a que el precio del activo suba.

---

## 4. Qué es una opción Put

Una **Put** es una opción que da el derecho a **vender** un activo a un precio fijo en el futuro.

Ejemplo simple:

* Una acción vale hoy 100.
* Tengo una Put que me permite venderla a 90 en el futuro.

Esta opción se vuelve más valiosa si el precio de la acción baja mucho.

Por ejemplo, si la acción baja a 60, poder venderla a 90 es conveniente.

En simple:

> Una Put apuesta a que el precio del activo baje.

---

## 5. Qué significa que una opción sea europea

Una **opción europea** es una opción que solo puede ejercerse en la fecha de vencimiento.

Esto no significa que sea una opción usada solo en Europa. Es una clasificación del contrato.

Existen otros tipos de opciones, como las opciones americanas, que pueden ejercerse en cualquier momento antes del vencimiento.

Para este módulo se trabajará con opciones europeas porque son más simples de modelar y son compatibles directamente con el modelo Black-Scholes clásico.

---

## 6. Qué es el precio Spot

El **Spot** es el precio actual del activo en el mercado.

Ejemplo:

* Si una acción vale hoy 100, entonces el spot es 100.

Este valor representa el punto de partida del cálculo.

En simple:

> Spot = precio actual del activo.

---

## 7. Qué es el Strike

El **Strike** es el precio fijo acordado en la opción.

Es el precio al que se podrá comprar o vender el activo en el futuro, dependiendo de si la opción es Call o Put.

Ejemplo para una Call:

* Spot actual: 100.
* Strike: 110.

La opción permite comprar el activo a 110 en el futuro.

Ejemplo para una Put:

* Spot actual: 100.
* Strike: 90.

La opción permite vender el activo a 90 en el futuro.

En simple:

> Strike = precio acordado de compra o venta futura.

---

## 8. Qué es la Maturity

La **Maturity** es el tiempo que falta hasta el vencimiento de la opción.

Normalmente se expresa en años.

Ejemplos:

* 1 año = 1.0
* 6 meses = 0.5
* 3 meses = 0.25
* 30 días ≈ 30 / 365

En simple:

> Maturity = cuánto tiempo falta para que venza la opción.

Para el endpoint Black-Scholes de esta primera versión, la maturity debe ser mayor que cero.
El caso exacto de vencimiento (`maturity = 0`) queda fuera de este endpoint porque el precio pasa a ser el payoff intrínseco y algunos Greeks pueden ser discontinuos o no estar definidos.

Mientras más tiempo queda, normalmente la opción puede valer más, porque existe más oportunidad de que el mercado se mueva a favor.

---

## 9. Qué es la tasa libre de riesgo

La **tasa libre de riesgo** representa una tasa de retorno teórica que se considera segura.

En la práctica, suele aproximarse usando instrumentos como bonos del gobierno.

Ejemplo:

* 5% anual se representa como 0.05.
* 10% anual se representa como 0.10.
* 2% anual se representa como 0.02.

En Black-Scholes, esta tasa se usa para descontar valores futuros al presente.

En simple:

> Risk-free rate = tasa usada como referencia para traer dinero futuro al valor actual.

---

## 10. Qué es la volatilidad

La **volatilidad** mide cuánto se mueve el precio de un activo.

Un activo con baja volatilidad se mueve poco.
Un activo con alta volatilidad se mueve mucho.

Ejemplo:

* Volatilidad de 20% anual se representa como 0.20.
* Volatilidad de 35% anual se representa como 0.35.

En general, mientras mayor es la volatilidad, más valiosa puede ser una opción.

Esto ocurre porque una opción se beneficia de movimientos grandes del mercado. Si el precio se mueve mucho, hay más posibilidad de que termine en una zona favorable.

En simple:

> Volatilidad = nivel de incertidumbre o movimiento esperado del activo.

---

## 11. Qué es Black-Scholes

**Black-Scholes** es un modelo matemático usado para estimar el precio teórico de opciones europeas.

El modelo recibe como entrada:

* Precio actual del activo.
* Strike.
* Tiempo hasta vencimiento.
* Tasa libre de riesgo.
* Volatilidad.
* Tipo de opción: Call o Put.

Y entrega como salida:

* Precio teórico de la opción.
* Sensibilidades llamadas Greeks.

En simple:

> Black-Scholes es una fórmula que estima cuánto debería valer una opción europea bajo ciertas condiciones.

---

## 12. Qué significa “sin dividend yield”

Algunos activos, como ciertas acciones, pagan dividendos.

El **dividend yield** representa el rendimiento esperado por dividendos.

En esta primera versión del módulo no se considerarán dividendos.

Eso significa que el modelo será más simple y asumirá que el activo no paga dividendos durante la vida de la opción.

En simple:

> Sin dividend yield = no se consideran dividendos en el cálculo.

---

## 13. Qué son los Greeks

Los **Greeks** son medidas que explican cómo cambia el precio de una opción cuando cambia alguna variable del mercado.

No son precios. Son sensibilidades.

Sirven para entender el comportamiento de la opción.

Los Greeks principales de este módulo son:

* Delta.
* Gamma.
* Vega.
* Theta.
* Rho.

En simple:

> Los Greeks ayudan a entender por qué cambia el precio de una opción.

---

## 14. Qué es Delta

**Delta** mide cuánto cambia el precio de la opción cuando cambia el precio del activo.

Ejemplo:

* Si una Call tiene Delta 0.60, significa que si el activo sube 1 unidad, la opción podría subir aproximadamente 0.60.

Para opciones Call, Delta suele estar entre 0 y 1.

Para opciones Put, Delta suele estar entre -1 y 0.

En simple:

> Delta mide qué tan sensible es la opción al precio del activo.

---

## 15. Qué es Gamma

**Gamma** mide cuánto cambia el Delta cuando cambia el precio del activo.

Delta mide la sensibilidad del precio de la opción.
Gamma mide la sensibilidad del Delta.

Ejemplo simple:

* Si Gamma es alto, el Delta puede cambiar rápidamente.
* Si Gamma es bajo, el Delta cambia más lentamente.

En simple:

> Gamma mide qué tan rápido cambia el Delta.

---

## 16. Qué es Vega

**Vega** mide cuánto cambia el precio de la opción cuando cambia la volatilidad.

Ejemplo:

* Si la volatilidad aumenta, normalmente el valor de la opción también aumenta.
* Vega ayuda a medir ese impacto.

Aunque se llama “Vega”, no es una letra griega. Se usa igual dentro del grupo de los Greeks por convención financiera.

En simple:

> Vega mide qué tan sensible es la opción a cambios en la volatilidad.

---

## 17. Qué es Theta

**Theta** mide cuánto cambia el precio de la opción con el paso del tiempo.

Normalmente, las opciones pierden valor a medida que se acerca su vencimiento.

Esto se conoce como pérdida de valor temporal.

Ejemplo:

* Una opción con mucho tiempo restante puede valer más.
* La misma opción, cerca del vencimiento, puede valer menos si el mercado no se movió a favor.

En simple:

> Theta mide el efecto del paso del tiempo sobre el precio de la opción.

---

## 18. Qué es Rho

**Rho** mide cuánto cambia el precio de la opción cuando cambia la tasa libre de riesgo.

Ejemplo:

* Si la tasa libre de riesgo sube, el precio de una Call puede cambiar.
* Rho mide ese impacto.

En simple:

> Rho mide qué tan sensible es la opción a cambios en la tasa de interés.

---

## 19. Qué significa que el cálculo sea stateless

Un cálculo **stateless** significa que no se guarda información entre una ejecución y otra.

Cada cálculo funciona de manera independiente.

La lógica es:

1. Se reciben los datos de entrada.
2. Se validan.
3. Se calcula el precio y los Greeks.
4. Se devuelve el resultado.
5. No se guarda nada en base de datos.

En simple:

> Stateless = cada request se calcula de forma independiente y no deja estado guardado.

---

## 20. Ejemplo conceptual completo

Supongamos que queremos calcular una opción Call con estos datos:

* Spot: 100
* Strike: 110
* Maturity: 1 año
* Risk-free rate: 5%
* Volatility: 20%
* Tipo: Call

La interpretación sería:

> Queremos estimar cuánto vale hoy el derecho a comprar un activo a 110 dentro de un año, sabiendo que hoy vale 100, que la tasa libre de riesgo es 5% y que la volatilidad esperada es 20%.

El modelo Black-Scholes toma esos datos y calcula:

* Precio teórico de la opción.
* Delta.
* Gamma.
* Vega.
* Theta.
* Rho.

---

## 21. Resumen de términos principales

| Término         | Explicación simple                                       |
| --------------- | -------------------------------------------------------- |
| Option          | Contrato que da derecho a comprar o vender un activo     |
| Call            | Derecho a comprar                                        |
| Put             | Derecho a vender                                         |
| European Option | Opción que solo se ejerce al vencimiento                 |
| Spot            | Precio actual del activo                                 |
| Strike          | Precio acordado para comprar o vender                    |
| Maturity        | Tiempo hasta el vencimiento                              |
| Risk-free rate  | Tasa usada como referencia segura                        |
| Volatility      | Qué tanto se mueve el activo                             |
| Black-Scholes   | Modelo para calcular precio teórico de opciones europeas |
| Greeks          | Medidas de sensibilidad del precio de la opción          |
| Delta           | Sensibilidad al precio del activo                        |
| Gamma           | Sensibilidad del Delta                                   |
| Vega            | Sensibilidad a la volatilidad                            |
| Theta           | Sensibilidad al paso del tiempo                          |
| Rho             | Sensibilidad a la tasa de interés                        |
| Stateless       | Cálculo independiente sin guardar datos                  |

---

## 22. Idea general del módulo

Este módulo busca implementar un cálculo financiero simple pero real.

No intenta simular todo un sistema financiero completo.
Tampoco busca conectarse a mercado real ni guardar operaciones.

La primera versión se enfoca en:

* Recibir datos básicos de una opción europea.
* Validar que los datos tengan sentido.
* Calcular precio teórico con Black-Scholes.
* Calcular Greeks principales.
* Devolver una respuesta clara.

En simple:

> El módulo transforma datos financieros básicos en un precio teórico de opción y sus principales sensibilidades.

Nota importante:

> Este endpoint trabaja con maturity estrictamente positiva. El payoff exacto al vencimiento se modelará por separado cuando el proyecto tenga una representación más completa de instrumentos y payoffs.
