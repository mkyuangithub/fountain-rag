import Request from "@/toolkit/request.js";
import Authorization from "@/toolkit/authorization.js";

class UserLoginApi extends Request {
  async doLogin(payload) {
    return this.post("/api/user/login", payload);
  }
  
  async checkLoginToken(payload) {
    return this.post("/api/user/checkLoginToken", payload,{
			token: payload.token,
		});
  }

  async logout(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/user/logout", payload, headers);
  }

  /*
  async listUserRepo(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/apapi/knowledge/list", payload, headers);
  }
    */
  async setLocale(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/user/setLocale", payload, headers);
  }
}



export default new UserLoginApi("/fountainbase");