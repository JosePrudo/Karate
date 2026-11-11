# Karate
Repositorio para practicar automatización con Karate

## Requisitos

- JDK 25 (LTS)
- Maven 3.9 o posterior

## Compilar y ejecutar las pruebas

```shell
mvn clean test
```

La suite también incluye pruebas de solo lectura para la API de Buggy Cars Rating:
consulta la marca Lamborghini, valida los modelos paginados y comprueba el 404
para una marca inexistente. Requiere acceso a Internet.

## Reporte de pruebas

Cada `mvn test` genera `reportes/Reporte_Pruebas.html` con todos los casos ejecutados,
gráficas, defectos, análisis y conclusiones. El archivo se sobrescribe en cada ejecución,
así que siempre muestra la última, y se genera aunque haya pruebas fallidas.

- El generador está en `src/test/java/reporte/ReporteGenerator.java` y la plantilla en
  `src/test/java/reporte/plantilla-reporte.html`.
- El ID del caso sale del tag `@CP-xx`, el módulo del primer tag funcional y el tipo de
  `@negativo` o `@borde`. Un fallo con `@defecto` se clasifica como defecto conocido
  (veredicto "Observado"); cualquier otro fallo da "No aprobado".
- El reporte solo se genera al ejecutar por Maven (`ApiTest`), no con `UsersRunner`.
