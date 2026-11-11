---
okf: "0.1"
id: "EJ-2026-11-11-01"
titulo: "Resultado de pruebas - API Buggy Cars Rating"
estado: ejecutado
fecha: 2026-11-11
ejecutor: copilot
lote_hu: "CP-1.1.1"
intento: 1
herramienta: "Karate 1.5.2 (mvn test, 5 hilos)"
---

# Resultado de pruebas - API Buggy Cars Rating

**Comando:** `mvn test "-Dkarate.options=classpath:api/buggycars"`
**Inicio:** 2026-11-11 11:41:48 · **Duración total:** 44 s · **Ambiente:** `karate.env=dev`
**Casos:** [CP-1.1.1-buggy-cars-api.md](../CP-1.1.1-buggy-cars-api.md)

## Resumen

| Casos | Iteraciones | Aprobados | Fallidos | Tasa de éxito | Veredicto |
|---|---|---|---|---|---|
| 20 | 23 | 19 | 1 | 95 % | Observado |

## Resultados por módulo

| Módulo | Casos | Aprobados | Fallidos |
|---|---|---|---|
| Marcas | 5 | 5 | 0 |
| Modelos | 2 | 2 | 0 |
| Ranking | 2 | 2 | 0 |
| Dashboard | 1 | 1 | 0 |
| Autenticación | 5 | 5 | 0 |
| Perfil | 1 | 1 | 0 |
| Votos | 2 | 2 | 0 |
| Registro | 2 | 1 | 1 |

## Defectos

- **DEF-01 (Medio):** `POST /users` acepta `password` y `confirmPassword` distintos (CP-20).

## Observaciones

- Latencia secuencial promedio: `/makes/{id}` 5,39 s; `/models/{id}` 4,63 s; `/models` 3,33 s; `/dashboard` 1,11 s.
- Hay comentarios con cargas XSS almacenadas sin sanear en la API.

## Evidencias

- [Reporte HTML con gráficas y conclusiones](./Reporte_Pruebas_API_Buggy_Cars.html)
- [Reporte técnico de Karate](./karate-reports/karate-summary.html)
- [Log de Maven](./ejecucion-maven.log)
- Capturas: [EV-feature-resumen.png](./EV-feature-resumen.png), [EV-CP-20-fallo.png](./EV-CP-20-fallo.png), [EV-timeline.png](./EV-timeline.png)
