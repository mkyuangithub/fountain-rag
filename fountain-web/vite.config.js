import {defineConfig, loadEnv} from "vite";
import vue from "@vitejs/plugin-vue";

import path from "path";

const consoleEnv = (env) => {
	console.log("")
	Object.keys(env).map(k => {
		console.log("\x1B[31m%s\x1B[0m", "全局配置 " + k + " = " + env[k]);
	});
	console.log("")
}

export default defineConfig(conf => {
	const env = loadEnv(conf.mode, process.cwd(), 'P_');
	consoleEnv(env);
	return {
		plugins: [vue()],
		resolve: {
			alias: {
				"@": path.resolve(__dirname, "src"),
				"vue-i18n": "vue-i18n/dist/vue-i18n.cjs.js"
			}
		},
		build: {
			outDir: "release",
			minify: "terser",
			chunkSizeWarningLimit: 1000,
			terserOptions: {
				compress: {
					drop_console: true,
					drop_debugger: true
				}
			},
			rollupOptions: {
				input: {
					main: "./index.html"
				},
				output: {
					manualChunks(id) {
						if (id.includes('node_modules')) {
							return id
								.toString()
								.split('node_modules/')[1]
								.split('/')[0]
								.toString();
						}
					}
				},
			}
		},
		define: {
			"process.env": env
		},
		server: {
			host: 'localhost',
			port: 8200,
			proxy: {
				"/fountaingateway/fountainbase":{
					target: "http://localhost:8080",
					rewrite: path => path.replace("/fountaingateway/fountainbase", ""),
					changeOrigin: true,
					ws: true
				},
				
			}
		}
	};
})
