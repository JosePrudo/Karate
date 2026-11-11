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
