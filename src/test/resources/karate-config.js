function fn() {
    var config = {};
    config.baseUrl = karate.properties['baseUrl'] || 'http://localhost:8081';
    return config;
}