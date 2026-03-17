
export default {
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:5001", // backend
        changeOrigin: true,
        secure: false
      }
    }
  }
};
