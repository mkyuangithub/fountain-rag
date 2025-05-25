import Request from "@/toolkit/request.js";
import Authorization from "@/toolkit/authorization.js";

class AIFunctionApi extends Request {

  async listAIFunction(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/function/list", payload, headers);
  }

  async addFunction(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/function/addFunction", payload, headers);
  }

  async updateFunction(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/function/updateFunction", payload, headers);
  }

  async deleteFunction(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/function/deleteFunction", payload, headers);
  }
}

export default new AIFunctionApi("/fountainbase");