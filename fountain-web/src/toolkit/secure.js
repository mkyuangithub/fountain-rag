import Settings from "@/toolkit/settings.js";
import CryptoJS from "crypto-js";

export const encrypt = (value) => {
	const iv = CryptoJS.enc.Utf8.parse('\x00'.repeat(16));
	const key = CryptoJS.enc.Utf8.parse(Settings.secure.key);
	let encrypted = CryptoJS.AES.encrypt(value, key, {
		iv: iv,
		mode: CryptoJS.mode.CBC,
		padding: CryptoJS.pad.Pkcs7
	});
	return CryptoJS.enc.Base64.stringify(encrypted.ciphertext);
}

/**对应后台的使用的是Base64.getUrlDecoder().decode(encryptTxt) */
export const encrypt_url = (value) => {
    const iv = CryptoJS.enc.Utf8.parse('\x00'.repeat(16));
    const key = CryptoJS.enc.Utf8.parse(Settings.secure.key);
    let encrypted = CryptoJS.AES.encrypt(value, key, {
        iv: iv,
        mode: CryptoJS.mode.CBC,
        padding: CryptoJS.pad.Pkcs7
    });
    // 将Base64转换为URL安全的格式
    return CryptoJS.enc.Base64.stringify(encrypted.ciphertext)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');
}

export const decrypt = (value) => {
	const iv = CryptoJS.enc.Utf8.parse('\x00'.repeat(16));
	const key = CryptoJS.enc.Utf8.parse(Settings.secure.key);
	let encryptedValue = CryptoJS.enc.Base64.parse(value);
	let decrypted = CryptoJS.AES.decrypt({ciphertext: encryptedValue}, key, {
		iv: iv,
		mode: CryptoJS.mode.CBC,
		padding: CryptoJS.pad.Pkcs7
	});
	return decrypted.toString(CryptoJS.enc.Utf8);
}