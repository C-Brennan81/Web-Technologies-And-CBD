function fn() {
  var baseUrl = java.lang.System.getProperty('baseUrl');
  if (!baseUrl) {
    baseUrl = 'http://localhost:8080';
  }
  return { baseUrl: baseUrl };
}