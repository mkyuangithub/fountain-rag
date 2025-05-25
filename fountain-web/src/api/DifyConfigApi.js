import Request from "@/toolkit/request.js";
import Authorization from "@/toolkit/authorization.js";
class DifyConfigApi extends Request {

  async listAllDifyConfigs(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/dify/listAllDifyConfigs", payload, headers);
  }
  async addDifyConfig(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/dify/addDifyConfig", payload, headers);
  }

  async updateDifyConfigs(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/dify/updateDifyConfigs", payload, headers);
  }
  async deleteDifyConfigs(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/dify/deleteDifyConfigs", payload, headers);
  }
}
export default new DifyConfigApi("/fountainbase");