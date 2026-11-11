function fn() {
  var env = karate.env || 'dev';
  karate.log('karate.env =', env);

  var config = {
    env: env,
    baseUrl: 'https://jsonplaceholder.typicode.com',
    buggyCarsBaseUrl: 'https://k51qryqov3.execute-api.ap-southeast-2.amazonaws.com/prod/makes',
    buggyApiUrl: 'https://k51qryqov3.execute-api.ap-southeast-2.amazonaws.com/prod',
    buggyUser: karate.properties['buggy.user'] || 'qa_tmp_x',
    buggyPassword: karate.properties['buggy.password'] || 'Abcdef1!'
  };

  if (env == 'qa') {
    config.baseUrl = 'https://jsonplaceholder.typicode.com';
  }

  karate.configure('connectTimeout', 10000);
  karate.configure('readTimeout', 30000);
  karate.configure('logPrettyRequest', true);
  karate.configure('logPrettyResponse', true);
  return config;
}
