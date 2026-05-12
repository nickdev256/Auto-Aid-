import React, {
  createContext,
  useContext,
  useEffect,
  useState,
} from "react";

import axios from "axios";

/* ======================================================
   CONTEXT
====================================================== */
const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

/* ======================================================
   API CONFIG
====================================================== */
const BASE_URL =
  import.meta.env.VITE_API_URL ||
  "http://localhost:5001";

const API = `${BASE_URL}/api/auth`;

/* ======================================================
   DEBUG LOGS
====================================================== */
console.log("API BASE URL:", BASE_URL);
console.log("AUTH API:", API);

/* ======================================================
   STORAGE KEYS
====================================================== */
const USER_STORAGE_KEY = "auth_user";
const TOKEN_STORAGE_KEY = "token";

/* ======================================================
   AXIOS DEFAULTS
====================================================== */
axios.defaults.withCredentials = true;

/* ======================================================
   AUTH PROVIDER
====================================================== */
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem(
        USER_STORAGE_KEY
      );

      return savedUser
        ? JSON.parse(savedUser)
        : null;
    } catch (err) {
      console.error(
        "Failed to parse saved user:",
        err
      );

      return null;
    }
  });

  const [authLoading, setAuthLoading] =
    useState(true);

  /* ======================================================
     SAVE AUTH DATA
  ====================================================== */
  const saveAuthData = (data) => {
    try {
      const token =
        data?.token ||
        data?.accessToken ||
        data?.jwt ||
        data?.user?.token ||
        null;

      const resolvedUser =
        data?.user || null;

      if (token) {
        localStorage.setItem(
          TOKEN_STORAGE_KEY,
          token
        );

        axios.defaults.headers.common[
          "Authorization"
        ] = `Bearer ${token}`;
      }

      if (resolvedUser) {
        localStorage.setItem(
          USER_STORAGE_KEY,
          JSON.stringify(resolvedUser)
        );
      }
    } catch (err) {
      console.error(
        "saveAuthData ERROR:",
        err
      );
    }
  };

  /* ======================================================
     CLEAR AUTH DATA
  ====================================================== */
  const clearAuthData = () => {
    localStorage.removeItem(
      TOKEN_STORAGE_KEY
    );

    localStorage.removeItem(
      USER_STORAGE_KEY
    );

    localStorage.removeItem(
      "adminToken"
    );

    localStorage.removeItem(
      "autoaid_token"
    );

    localStorage.removeItem(
      "autoaid_user"
    );

    delete axios.defaults.headers.common[
      "Authorization"
    ];
  };

  /* ======================================================
     LOAD TOKEN ON START
  ====================================================== */
  useEffect(() => {
    const token = localStorage.getItem(
      TOKEN_STORAGE_KEY
    );

    if (token) {
      axios.defaults.headers.common[
        "Authorization"
      ] = `Bearer ${token}`;
    }
  }, []);

  /* ======================================================
     CHECK AUTH
  ====================================================== */
  const checkAuth = async () => {
    try {
      console.log(
        "CHECKING AUTH AT:",
        `${API}/me`
      );

      const res = await axios.get(
        `${API}/me`,
        {
          withCredentials: true,
        }
      );

      console.log(
        "AUTH RESPONSE:",
        res.data
      );

      const me =
        res?.data?.user || null;

      setUser(me);

      if (me) {
        localStorage.setItem(
          USER_STORAGE_KEY,
          JSON.stringify(me)
        );
      } else {
        localStorage.removeItem(
          USER_STORAGE_KEY
        );
      }
    } catch (err) {
      console.error(
        "AUTH CHECK FAILED:",
        err
      );

      console.log(
        "AUTH ERROR RESPONSE:",
        err?.response?.data
      );

      console.log(
        "AUTH ERROR MESSAGE:",
        err?.message
      );

      setUser(null);

      clearAuthData();
    } finally {
      setAuthLoading(false);
    }
  };

  /* ======================================================
     INITIAL AUTH CHECK
  ====================================================== */
  useEffect(() => {
    checkAuth();
  }, []);

  /* ======================================================
     LOGIN
  ====================================================== */
  const login = async (
    email,
    password
  ) => {
    try {
      console.log(
        "LOGIN REQUEST URL:",
        `${API}/login`
      );

      const res = await axios.post(
        `${API}/login`,
        {
          email: (email || "")
            .trim()
            .toLowerCase(),

          password:
            password || "",
        },
        {
          withCredentials: true,
        }
      );

      console.log(
        "LOGIN RESPONSE:",
        res.data
      );

      const loggedInUser =
        res?.data?.user || null;

      saveAuthData(res.data);

      setUser(loggedInUser);

      return loggedInUser;
    } catch (err) {
      console.error(
        "LOGIN ERROR FULL:",
        err
      );

      console.log(
        "LOGIN ERROR RESPONSE:",
        err?.response?.data
      );

      console.log(
        "LOGIN ERROR MESSAGE:",
        err?.message
      );

      const msg =
        err?.response?.data
          ?.message ||
        err?.message ||
        "Login failed";

      throw new Error(msg);
    }
  };

  /* ======================================================
     SIGNUP
  ====================================================== */
  const signup = async (
    formData
  ) => {
    try {
      console.log(
        "SIGNUP REQUEST URL:",
        `${API}/signup`
      );

      const res = await axios.post(
        `${API}/signup`,
        formData,
        {
          withCredentials: true,
        }
      );

      console.log(
        "SIGNUP RESPONSE:",
        res.data
      );

      return res.data;
    } catch (err) {
      console.error(
        "SIGNUP ERROR FULL:",
        err
      );

      console.log(
        "SIGNUP ERROR RESPONSE:",
        err?.response?.data
      );

      console.log(
        "SIGNUP ERROR MESSAGE:",
        err?.message
      );

      const msg =
        err?.response?.data
          ?.message ||
        err?.message ||
        "Signup failed";

      throw new Error(msg);
    }
  };

  /* ======================================================
     LOGOUT
  ====================================================== */
  const logout = async () => {
    try {
      await axios.post(
        `${API}/logout`,
        {},
        {
          withCredentials: true,
        }
      );
    } catch (err) {
      console.warn(
        "Logout request failed:",
        err?.message
      );
    } finally {
      clearAuthData();

      setUser(null);
    }
  };

  /* ======================================================
     CONTEXT VALUE
  ====================================================== */
  const value = {
    user,
    setUser,
    login,
    signup,
    logout,
    authLoading,
    checkAuth,
    isAuthenticated: !!user,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;