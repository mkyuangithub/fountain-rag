import Settings from "@/toolkit/settings.js";

const formatMenu = (list = []) => {
	let result = [];
	for (let i = 0; i < list.length; i++) {
		let item = list[i];
		if (item.menuUrl == "/center") {
			result.push({
				name: item.menuTxt,
				icon: "HomeOutlined",
				code: "Center",
				match: ["Center"]
			});
		} else if (item.menuUrl == "/article") {
			result.push({
				name: item.menuTxt,
				icon: "FileTextOutlined",
				code: "Article",
				match: ["Article", "ArticleCreate", "ArticleUpdate"]
			});
		} else if (item.menuUrl == "/member") {
			result.push({
				name: item.menuTxt,
				icon: "SettingOutlined",
				code: "Member",
				match: ["Member"]
			});
		}
	}
	return result;
}

export const isLogin = () => {
	let item = sessionStorage.getItem("user");
	if (isBlank(item)) {
		return false;
	}
	let user = JSON.parse(item);
	if (isBlank(user.id)) {
		return false;
	}
	return true;
}

export const isBlank = (value) => {
	if (null == value || value == undefined || value == "") {
		return true;
	}
	return false;
};

export const isPhoneNumber = (value) => {
	const phoneReg = /^[1][3,4,5,6,7,8][\d]{9}$/;
	return phoneReg.test(value);
}

export const findResource = (list = []) => {
	for (let i = 0; i < list.length; i++) {
		let item = list[i];
		if (item.menuTxt == Settings.resource.root) {
			return formatMenu(item.children);
		}
	}
	return [];
}

export const formatBytes = (bytes, decimals = 2) => {
	if (bytes === 0) return "0 Bytes";
	const k = 1024;
	const dm = decimals < 0 ? 0 : decimals;
	const sizes = ["Bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"];
	const i = Math.floor(Math.log(bytes) / Math.log(k));
	return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + " " + sizes[i];
}

export const substring = (value, limit = 20, append = "") => {
	if (value && value != "") {
		if (value.length > limit) {
			return value.substring(0, limit) + append;
		} else {
			return value;
		}
	} else {
		return "";
	}
}