import Request from "@/toolkit/request.js";
import Authorization from "@/toolkit/authorization.js";

class SmartPurchaseApi extends Request {
  
async getGoodsByIdList(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/demo/getGoodsByIdList", payload, headers);
  }
}
export default new SmartPurchaseApi("/fountainbase");