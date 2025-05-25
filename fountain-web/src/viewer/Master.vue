<template>
	<div class="master">
		<router-view />
	</div>
</template>
<script setup>
import { onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import Authorization from "@/toolkit/authorization.js";
import UserLoginApi from "@/api/UserLoginApi.js";
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "../toolkit/authorization";
import { message } from "ant-design-vue";
const route = useRoute();
const router = useRouter();

const intoMaster = () => {
	if (route.name == "Master" || route.name == "Center") {
		let query = route.query || {};
		let params = route.params;
		let channel = params.channel || "default";
		router.replace({
			name: "Center",
			query: query,
			params: {
				channel
			}
		});
	}
}
const checkLoginInfo = async () => {
    try {
        const userSession = Authorization.get();
        console.log("userSession->", userSession);

        if (!userSession || userSession === null) {
            let channel = route.params.channel || 'default';
            await router.replace({
                name: 'Login',
                params: {
                    channel
                }
            });
            return false;
        }
        
        let token = authorization.getToken();
        let encryptedToken = encrypt_url(token);
        let payload = {
            token: encryptedToken,
        }
        
        // 使用 await 等待 Promise 结果
        const res = await UserLoginApi.checkLoginToken(payload);
        console.log(">>>>>>after checkLoginToken the result is->", JSON.stringify(res));
        const finalResult = res;
        console.log("最终返回值：", finalResult);
        return finalResult;
    } catch (err) {
        console.log(">>>>>>checkLoginInfo error->", err);
        return false;
    }
}


onMounted(async () => {
	try {
		const isLoggedIn = await checkLoginInfo();
		console.log(">>>>>>isLoggedIn->"+isLoggedIn);
		if (isLoggedIn) {
			intoMaster();
		}else{
			let channel = route.params.channel || 'default';
			await router.replace({
				name: 'Login',
				params: {
					channel
				}
			});
			return false;
		}
	} catch (err) {
		console.error(">>>>>>inital to Master error->", err);
	}
});
</script>
<style scoped>
.master {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background-color: #f7f7f7;
}
</style>