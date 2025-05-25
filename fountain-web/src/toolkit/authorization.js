import dayjs from "dayjs";

import {encrypt, decrypt} from "@/toolkit/secure.js";

const KEY = "userSession";
const EXP = 3;//3天
const EXP_UNIT = "day";

class Authorization {
	set(item) {
		let token = {
			expire: dayjs().add(EXP, EXP_UNIT).format("YYYY-MM-DD HH:mm:ss"),
			...(item || {})
		};
		let value = encrypt(JSON.stringify(token));
		localStorage.setItem(KEY, value);
		// localStorage.setItem("ut_1", JSON.stringify(token));
		// sessionStorage.setItem(KEY, JSON.stringify(item || {}));
	}

	get() {
		let value = localStorage.getItem(KEY);
		if (value && value != "") {
			try {
				return decrypt(value);
			} catch (e) {
				return null;
			}
		} else {
			return null;
		}
		// return sessionStorage.getItem(KEY) || localStorage.getItem(KEY)
	}

	getToken() {
		const text = this.get();
		if (!text) return undefined
		try {
			const data = JSON.parse(text);
			if (dayjs().isBefore(dayjs(data.expire))) {
				return data.token;
			} else {
				return undefined;
			}
		} catch (e) {
			return undefined
		}
	}

	getUserName() {
		const text = this.get();
		if (!text) return undefined
		try {
			const data = JSON.parse(text)
			if (dayjs().isBefore(dayjs(data.expire))) {
				return data.userName;
			} else {
				return undefined;
			}
		} catch (e) {
			return undefined
		}
	}
}

export default new Authorization()
