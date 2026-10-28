import axios from "axios";
import { attachRoleToken, clearExpiredRequestSession } from "./sessionAuth";

const adminAxiosInstance = axios.create({ baseURL: import.meta.env.VITE_API_URL });

adminAxiosInstance.interceptors.request.use(
  (config) => attachRoleToken(config, "admin"),
  (error) => Promise.reject(error)
);

adminAxiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const expiredRole = clearExpiredRequestSession(error.config);
      if (expiredRole === "admin" &&
          window.location.pathname.startsWith("/admin/") &&
          window.location.pathname !== "/admin/login") {
        window.location.assign("/admin/login");
      }
    }
    return Promise.reject(error);
  }
);

export default adminAxiosInstance;
