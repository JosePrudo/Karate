@buggy-cars
Feature: API de Buggy Cars Rating

  Background:
    * url buggyCarsBaseUrl
    * def makeId = 'ckl2phsabijs71623vk0'

  @smoke
  Scenario: Consultar Lamborghini y los modelos de la primera pagina
    Given path makeId
    And param modelsPage = 1
    When method get
    Then status 200
    And match response.name == 'Lamborghini'
    And match response.description == '#string'
    And match response.image == '#string'
    And match response.models.totalPages == '#number'
    And match response.models.models == '#[]'
    And match each response.models.models contains
      """
      {
        id: '#string',
        name: '#string',
        image: '#string',
        make: 'Lamborghini',
        makeId: '#string',
        makeImage: '#string',
        votes: '#number',
        rank: '#number',
        engineVol: '#number',
        comments: '#[]',
        totalComments: '#number'
      }
      """

  Scenario: Consultar los modelos de la segunda pagina
    Given path makeId
    And param modelsPage = 2
    When method get
    Then status 200
    And match response.name == 'Lamborghini'
    And match response.models.totalPages == '#number'
    And match response.models.models == '#[]'
    And match each response.models.models contains
      """
      {
        id: '#string',
        name: '#string',
        make: 'Lamborghini',
        makeId: '#string'
      }
      """

  Scenario: Consultar una marca inexistente
    Given path 'nonexistent'
    And param modelsPage = 1
    When method get
    Then status 404
