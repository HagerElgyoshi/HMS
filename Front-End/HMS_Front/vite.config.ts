import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";
import { componentTagger } from "lovable-tagger";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  server: {
    host: "::",
    port: 5173,
    hmr: {
      overlay: false,
    },
    proxy: {
      // Dev-only proxy for the AI chatbot. Target is read from the environment
      // (VITE_CHATBOT_PROXY_TARGET, centralized in .env.production).
      // If not set, the proxy block is effectively disabled (empty target).
      "/chatbot-api": {
        target: process.env.VITE_CHATBOT_PROXY_TARGET || "",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/chatbot-api/, ""),
      },
    },
  },
  plugins: [react(), mode === "development" && componentTagger()].filter(Boolean),
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
}));
