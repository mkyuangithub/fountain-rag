import axios from "axios";
import { fetchEventSource } from "@microsoft/fetch-event-source";
import settings from "@/toolkit/settings.js";
import authorization from "@/toolkit/authorization.js";
import { message } from "ant-design-vue";

//import { useLoginStore } from "@/stores/UseLoginStore.js";
import { encrypt, encrypt_url, decrypt } from "@/toolkit/secure.js";

axios.defaults.baseURL = settings.request.baseurl;
axios.defaults.timeout = 600000;


axios.interceptors.request.use(
	config => {
		// 基础认证header
		if (authorization.getToken()) {
			config.headers.token = encrypt_url(authorization.getToken());
		}

		return config;
	}
);
const AUTH_ERROR_CODES = [1001, 1002, 1003, 1004];

// 添加一个防抖控制变量用于不要重复显示错误
let lastErrorTime = 0;
const ERROR_COOLDOWN = 2000; // 2秒内不重复显示相同错误

const httpSuccess = (res, resolve, reject) => {
	//console.log(res)
	if (res && res.data) {
		let body = res.data;
		try {
			if (body && body.hasOwnProperty("code") && (body.code == 0 || body.code == 4001 || body.code === '000000')) {
				resolve(body.data);
			} else {
				if ((body.code && AUTH_ERROR_CODES.includes(Number(body.code)))) {
					const now = Date.now();
					if (now - lastErrorTime > ERROR_COOLDOWN) {
						message.error("您当前的帐号无此操作权限,请重新登录");
						lastErrorTime = now;
					}
					//let loginStore = useLoginStore();
					//loginStore.increment();
					//return;
					reject(body.data);
					return;
				}
				if (body.code && body.code == "-1" && body.message == "Missing request header 'ut' for method parameter of type String") {
					//message.error("您的登录状态已失效,请重新登录");
					//reject(body.data);
					//window.location.reload();
					//let loginStore = useLoginStore();
					//loginStore.increment();
					return;
				} else {
					//message.error(body.message || body.desc);
					reject(body.data);
				}
			}
		} catch (e) {
			reject({
				code: -1,
				success: false,
				failure: true,
				message: e.message
			});
		}
	} else {
		reject({
			code: -1,
			success: false,
			failure: true,
			message: '服务异常'
		});
	}
}

const httpFailure = (res, reject) => {
	if (res && res.response) {
		let status = res.response.status;
		let statusText = res.response.statusText;
		if (status === 400) {
			reject({
				code: '400',
				success: false,
				failure: true,
				message: statusText
			});
		} else if (status === 401) {
			reject({
				code: '401',
				success: false,
				failure: true,
				message: '未授权,请登录'
			});
		} else if (status === 403) {
			reject({
				code: '403',
				success: false,
				failure: true,
				message: '拒绝访问'
			});
		} else if (status === 404) {
			reject({
				code: '404',
				success: false,
				failure: true,
				message: '请求服务不存在'
			});
		} else if (status === 408) {
			reject({
				code: '408',
				success: false,
				failure: true,
				message: '服务请求超时'
			});
		} else if (status === 500) {
			reject({
				code: '500',
				success: false,
				failure: true,
				message: '服务内部错误'
			});
		} else if (status === 501) {
			reject({
				code: '501',
				success: false,
				failure: true,
				message: '服务未实现'
			});
		} else if (status === 502) {
			reject({
				code: '502',
				success: false,
				failure: true,
				message: '网关错误'
			});
		} else if (status === 503) {
			reject({
				code: '503',
				success: false,
				failure: true,
				message: '服务不可用，服务器暂时过载或维护。'
			});
		} else if (status === 504) {
			reject({
				code: '504',
				success: false,
				failure: true,
				message: '网关超时'
			});
		} else if (status === 505) {
			reject({
				code: '505',
				success: false,
				failure: true,
				message: 'HTTP版本不受支持'
			});
		} else {
			reject({
				code: '9999',
				success: false,
				failure: true,
				message: statusText
			});
		}
	} else {
		if (res && res.code === 'ECONNABORTED') {
			reject({
				code: '408',
				success: false,
				failure: true,
				message: '服务请求超时'
			});
		} else if (res && res.code === 'ECONNREFUSED') {
			reject({
				code: '9999',
				success: false,
				failure: true,
				message: '服务不通'
			});
		} else {
			console.log("后台api调用出错了->"+JSON.stringify(res));
			reject({
				code: '9999',
				success: false,
				failure: true,
				message: '其他异常'
			});
		}
	}
}

export default class Request {

	constructor(prefix) {
		this.prefix = prefix || settings.request.context;
	}

	sse(url, params, headers, options) {
		let address = settings.request.baseurl + this.prefix + url;
		let ut = authorization.getToken();
		if (null == ut || ut == undefined || ut == "") {
			message.error("您的登录状态已失效,请重新登录");

			//let loginStore = useLoginStore();
			//loginStore.increment();
			//window.location.reload();
			return Promise.reject({ code: "-1", message: "您的登录状态已失效,请重新登录" });
		}
		return fetchEventSource(address, {
			body: JSON.stringify(params || {}),
			method: "post",
			headers: {
				ut: authorization.getToken(),
				//loginId: authorization.getLoginId(),
				"Content-Type": "application/json",
				...(headers || {})
			},
			openWhenHidden: true,
			async onopen(response) {
				if (options && options.onStart) {
					options.onStart(response);
				}
			},
			async onerror(error) {
				if (options && options.onError()) {
					options.onError(error);
				}
				throw new Error(error);
			},
			async onclose() {
				if (options && options.onClose) {
					options.onClose();
				}
			},
			async onmessage({ data, event }) {
				if (options && options.onMessage) {
					options.onMessage(data);
				}
			}
		})
	}

	get(url, params, headers, options) {
		return new Promise((resolve, reject) => {
			axios({
				url: this.prefix + url,
				params: params,
				method: "get",
				headers: headers || {},
				...(options || {})
			}).then((res) => {
				httpSuccess(res, resolve, reject);
			}).catch((err) => {
				httpFailure(err, reject);
			});
		})
	}

	post(url, params, headers, options) {
		return new Promise((resolve, reject) => {
			axios({
				url: this.prefix + url,
				data: params,
				method: "post",
				headers: headers || {},
				...(options || {})
			}).then((res) => {
				if (options && options.responseType === 'blob') {
					console.log(">>>>>>有blob内容返回");
					resolve(res.data);
				} else {
					// 否则走正常的 JSON 响应处理流程
					httpSuccess(res, resolve, reject);
				}
				//httpSuccess(res, resolve, reject);
			}).catch((err) => {
				httpFailure(err, reject);
			});
		})
	}

	rawPost(url, params, headers) {
		return new Promise((resolve, reject) => {
			axios({
				url: this.prefix + url,
				method: "post",
				data: params,
				headers: headers
			}).then((res) => {
				// 新增：检查是否为二进制响应
				if (headers?.responseType === 'blob') {
					// 如果是blob类型，直接返回响应数据
					resolve(res.data);
					return;
				}

				let body = res.data;
				try {
					if (body && body.hasOwnProperty("code") && (body.code == 0 || body.code == 4001 || body.code === '000000')) {
						resolve({
							success: true,
							...res.data
						});
					} else {
						if (body.code && body.code == "-1" && body.message == "Missing request header 'ut' for method parameter of type String") {
							message.error("您的登录状态已失效,请重新登录");
							reject(body.data);
							//window.location.reload();
							//let loginStore = useLoginStore();
							//loginStore.increment();
						} else {
							message.error(body.message || body.desc);
							reject(body.data);
						}
					}
				} catch (e) {
					reject({
						code: -1,
						success: false,
						failure: true,
						message: e.message
					});
				}

				// if (res && res.data && (res.data.code === 0 || res.data.code === 4001 || res.data.code === '000000')) {
				// 	resolve({
				// 		success: true,
				// 		...res.data
				// 	});
				// } else {
				// 	reject({
				// 		success: false,
				// 		...res
				// 	});
				// }
			}).catch((res) => {
				reject({
					success: false,
					...res
				});
			});
		})
	}

}