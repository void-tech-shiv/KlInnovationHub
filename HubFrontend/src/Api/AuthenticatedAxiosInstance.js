import axios from "axios";
import {
  attachRoleToken,
  clearExpiredRequestSession,
  getSharedAuthRole,
} from "./sessionAuth";

const authenticatedAxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

authenticatedAxiosInstance.interceptors.request.use(
  (config) => attachRoleToken(config, config.authRole || getSharedAuthRole()),
  (error) => Promise.reject(error)
);

authenticatedAxiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const expiredRole = clearExpiredRequestSession(error.config);
      if (expiredRole &&
          (window.location.pathname === "/profile" ||
            window.location.pathname.startsWith("/profile/"))) {
        window.location.assign("/login");
      }
    }
    return Promise.reject(error);
  }
);

export default authenticatedAxiosInstance;
