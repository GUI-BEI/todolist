import axios from 'axios';

const http = axios.create({
  baseURL: 'http://localhost:8080/api', // Spring Boot 后端接口基础地址
  timeout: 5000
});

export default http;
