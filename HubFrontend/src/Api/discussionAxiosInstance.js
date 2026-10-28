import axios from "axios";
import {
  attachRoleToken,
  clearExpiredRequestSession,
  getSharedAuthRole,
} from "./sessionAuth";

const discussionAxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

discussionAxiosInstance.interceptors.request.use(
  (config) => attachRoleToken(config, config.authRole || getSharedAuthRole()),
  (error) => Promise.reject(error)
);

discussionAxiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // A public discussion request may be unauthenticated: only clear the
      // role/token actually used on a failed authenticated request.
      clearExpiredRequestSession(error.config);
    }
    return Promise.reject(error);
  }
);

export default discussionAxiosInstance;
