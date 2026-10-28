import axios from "axios";
import { attachRoleToken, clearExpiredRequestSession } from "./sessionAuth";

const axiosInstance = axios.create({ baseURL: import.meta.env.VITE_API_URL });

axiosInstance.interceptors.request.use(
  (config) => attachRoleToken(config, "student"),
  (error) => Promise.reject(error)
);

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const expiredRole = clearExpiredRequestSession(error.config);
      if (expiredRole === "student") {
        const path = window.location.pathname;
        const needsStudentLogin =
          path === "/submitProject" ||
          path === "/formATeam" ||
          path === "/teamApplications" ||
          path === "/profile" ||
          path.startsWith("/profile/");
        if (needsStudentLogin) {
          window.location.assign("/login");
        }
      }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
