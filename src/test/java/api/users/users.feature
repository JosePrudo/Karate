@users
Feature: API de usuarios (JSONPlaceholder)

  Background:
    * url baseUrl
    * path 'users'

  @smoke
  Scenario: Listar usuarios
    When method get
    Then status 200
    And match response == '#[10]'
    And match each response contains { id: '#number', name: '#string', email: '#string' }

  Scenario: Obtener un usuario por id
    * path '1'
    When method get
    Then status 200
    And match response.id == 1
    And match response.username == 'Bret'

  Scenario: Crear un usuario
    * def body = read('classpath:api/users/data/nuevo-usuario.json')
    Given request body
    When method post
    Then status 201
    And match response contains body
    And match response.id == '#number'

  Scenario Outline: Consultar usuario <id>
    * path '<id>'
    When method get
    Then status <estado>

    Examples:
      | id  | estado |
      | 2   | 200    |
      | 999 | 404    |
