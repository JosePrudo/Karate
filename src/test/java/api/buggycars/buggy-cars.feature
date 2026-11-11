@buggy-cars-regresion
Feature: Buggy Cars Rating - regresion de API (20 casos)

  Background:
    * url buggyApiUrl
    * def lamborghiniId = 'ckl2phsabijs71623vk0'
    * def paganiId = 'ckl2phsabijs7162403g'
    * def diabloId = 'ckl2phsabijs71623vk0|ckl2phsabijs71623vqg'
    * def modeloSchema =
      """
      {
        id: '#string', name: '#string', image: '#string', make: '#string', makeId: '#string',
        makeImage: '#string', votes: '#number', rank: '#number', engineVol: '#number',
        comments: '#[]', totalComments: '#number'
      }
      """
    * def login =
      """
      function() {
        var r = karate.http(buggyApiUrl + '/oauth/token')
          .header('Content-Type', 'application/x-www-form-urlencoded')
          .post('grant_type=password&username=' + buggyUser + '&password=' + encodeURIComponent(buggyPassword));
        return r.body.access_token;
      }
      """

  @CP-01 @smoke @marcas
  Scenario: CP-01 Consultar Lamborghini con modelos de la pagina 1
    Given path 'makes', lamborghiniId
    And param modelsPage = 1
    When method get
    Then status 200
    And match response.name == 'Lamborghini'
    And match response.description == '#string'
    And match response.image == '#string'
    And match response.models.totalPages == '#number'
    And match response.models.models == '#[_ > 0]'
    And match each response.models.models == modeloSchema
    And match each response.models.models contains { make: 'Lamborghini', makeId: '#(lamborghiniId)' }

  @CP-02 @marcas
  Scenario: CP-02 Consultar los modelos de la pagina 2 de Lamborghini
    Given path 'makes', lamborghiniId
    And param modelsPage = 2
    When method get
    Then status 200
    And match response.models.models == '#[_ > 0]'
    And match each response.models.models contains { make: 'Lamborghini' }

  @CP-03 @marcas @borde
  Scenario: CP-03 Pagina de modelos fuera de rango devuelve lista vacia
    Given path 'makes', lamborghiniId
    And param modelsPage = 99
    When method get
    Then status 200
    And match response.name == 'Lamborghini'
    And match response.models.models == []
    And match response.models.totalPages == '#number'

  @CP-04 @marcas
  Scenario: CP-04 Consultar otra marca (Pagani) solo devuelve sus modelos
    Given path 'makes', paganiId
    And param modelsPage = 1
    When method get
    Then status 200
    And match response.name == 'Pagani'
    And match response.models.models == '#[_ > 0]'
    And match each response.models.models contains { make: 'Pagani', makeId: '#(paganiId)' }

  @CP-05 @marcas @negativo
  Scenario: CP-05 Consultar una marca inexistente devuelve 404
    Given path 'makes', 'nonexistent'
    And param modelsPage = 1
    When method get
    Then status 404
    And match response == { message: 'Not Found' }

  @CP-06 @modelos @smoke
  Scenario: CP-06 Consultar el detalle del modelo Diablo sin autenticacion
    Given path 'models', diabloId
    When method get
    Then status 200
    And match response.name == 'Diablo'
    And match response.description == '#string'
    And match response.votes == '#number'
    And match response.engineVol == '#number'
    And match response.comments == '#[]'
    And match each response.comments contains { user: '#string', datePosted: '#string', text: '#string' }
    And match response.canVote == false

  @CP-07 @modelos @negativo
  Scenario: CP-07 Consultar un modelo inexistente devuelve 404
    Given path 'models', lamborghiniId + '|noexiste'
    When method get
    Then status 404
    And match response == { message: 'Not Found' }

  @CP-08 @ranking
  Scenario: CP-08 Ranking general ordenado por posicion
    Given path 'models'
    And param page = 1
    And param orderBy = 'rank'
    When method get
    Then status 200
    And match response.totalPages == '#number'
    And match response.models == '#[_ > 0]'
    And match each response.models == modeloSchema
    * def ranks = $response.models[*].rank
    * def ordenados = karate.sort(ranks, function(x){ return x })
    And match ranks == ordenados

  @CP-09 @ranking
  Scenario: CP-09 Ranking general ordenado por marca
    Given path 'models'
    And param page = 1
    And param orderBy = 'make'
    When method get
    Then status 200
    * def marcas = $response.models[*].make
    * def ordenadas = karate.sort(marcas, function(x){ return x.toLowerCase() })
    And match marcas == ordenadas

  @CP-10 @dashboard @smoke
  Scenario: CP-10 Dashboard muestra la marca y el modelo mas populares
    Given path 'dashboard'
    When method get
    Then status 200
    And match response.make == { id: '#string', name: '#string', image: '#string', votes: '#number' }
    And match response.model == { id: '#string', make: '#string', name: '#string', image: '#string', votes: '#number' }
    And match response.model.id == '#regex .+\\|.+'

  @CP-11 @auth @smoke
  Scenario: CP-11 Login con credenciales validas devuelve token Bearer
    Given path 'oauth', 'token'
    And form field grant_type = 'password'
    And form field username = buggyUser
    And form field password = buggyPassword
    When method post
    Then status 200
    And match response.access_token == '#string'
    And match response.refresh_token == '#string'
    And match response.token_type == 'Bearer'
    And match response.expires_in == 3600

  @CP-12 @auth @negativo
  Scenario: CP-12 Login con credenciales invalidas devuelve 401
    Given path 'oauth', 'token'
    And form field grant_type = 'password'
    And form field username = 'usuario_inexistente_qa'
    And form field password = 'Clave123!'
    When method post
    Then status 401
    And match response == 'Invalid credentials'

  @CP-13 @auth
  Scenario: CP-13 Consultar el usuario actual con token valido
    * def token = login()
    Given path 'users', 'current'
    And header Authorization = 'Bearer ' + token
    When method get
    Then status 200
    And match response == { firstName: 'Qa', lastName: 'Test', isAdmin: false }

  @CP-14 @auth @negativo
  Scenario: CP-14 Consultar el usuario actual sin token devuelve 401
    Given path 'users', 'current'
    When method get
    Then status 401
    And match response == { message: 'Not authorized.' }

  @CP-15 @auth @negativo
  Scenario: CP-15 Consultar el usuario actual con token invalido devuelve 401
    Given path 'users', 'current'
    And header Authorization = 'Bearer token-invalido'
    When method get
    Then status 401
    And match response == { message: 'Not authorized.' }

  @CP-16 @perfil
  Scenario: CP-16 Consultar el perfil del usuario autenticado
    * def token = login()
    Given path 'users', 'profile'
    And header Authorization = 'Bearer ' + token
    When method get
    Then status 200
    And match response contains { username: '#(buggyUser)', firstName: 'Qa', lastName: 'Test' }
    And match response contains { gender: '#string', age: '#string', address: '#string', phone: '#string', hobby: '#string' }

  @CP-17 @votos
  Scenario: CP-17 Usuario autenticado que no ha votado puede votar el modelo
    * def token = login()
    Given path 'models', diabloId
    And header Authorization = 'Bearer ' + token
    When method get
    Then status 200
    And match response.canVote == true

  @CP-18 @votos @negativo
  Scenario: CP-18 Votar sin autenticacion devuelve 401
    Given path 'models', diabloId, 'vote'
    And request { comment: 'Voto sin autenticacion' }
    When method post
    Then status 401
    And match response == { message: 'Not authorized.' }

  @CP-19 @registro @negativo
  Scenario Outline: CP-19 Registro con datos invalidos es rechazado - <caso>
    Given path 'users'
    And request <cuerpo>
    When method post
    Then status 400
    And match response.message contains '<mensaje>'

    Examples:
      | caso               | cuerpo                                                                                                         | mensaje                 |
      | sin username       | {}                                                                                                             | username is required    |
      | sin password       | { username: 'qa_tmp_nopass', firstName: 'Qa', lastName: 'Test' }                                               | password is required    |
      | password corta     | { username: 'qa_tmp_short', firstName: 'Qa', lastName: 'Test', password: 'abc', confirmPassword: 'abc' }       | minimum field size of 6 |
      | username duplicado | { username: 'qa_tmp_x', firstName: 'Qa', lastName: 'Test', password: 'Abcdef1!', confirmPassword: 'Abcdef1!' } | User already exists     |

  @CP-20 @registro @negativo @defecto
  Scenario: CP-20 Registro con contrasena y confirmacion distintas es rechazado
    Given path 'users'
    And request { username: 'qa_tmp_mismatch', firstName: 'Qa', lastName: 'Test', password: 'Abcdef1!', confirmPassword: 'Xbcdef1!' }
    When method post
    Then status 400
    And match response.message == '#string'
    And match response.message !contains 'User already exists'
