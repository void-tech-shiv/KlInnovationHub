import axios from "axios";
import { attachRoleToken, clearExpiredRequestSession } from "./sessionAuth";

const reviewerAxiosInstance = axios.create({ baseURL: import.meta.env.VITE_API_URL });

reviewerAxiosInstance.interceptors.request.use(
  (config) => attachRoleToken(config, "reviewer"),
  (error) => Promise.reject(error)
);

reviewerAxiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const expiredRole = clearExpiredRequestSession(error.config);
      if (expiredRole === "reviewer" &&
          window.location.pathname.startsWith("/reviewer")) {
        window.location.assign("/login");
      }
    }
    return Promise.reject(error);
  }
);

export default reviewerAxiosInstance;
