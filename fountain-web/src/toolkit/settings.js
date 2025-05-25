import dayjs from "dayjs";
import {ConfigProvider} from "ant-design-vue";




export default {
	debug: process.env.P_DEBUG,
	secure:{
		key:process.env.P_SECURE_KEY
	},
	request: {
		baseurl: process.env.P_REQUEST_API_BASEURL,
		timeout: process.env.P_REQUEST_API_TIMEOUT,
		context: process.env.P_REQUEST_API_CONTEXT,
		previewImgUrl: process.env.P_BASE_PIC_PREVIEW_URL,
	},
	resource: {
		root: "fountain"
	}
}