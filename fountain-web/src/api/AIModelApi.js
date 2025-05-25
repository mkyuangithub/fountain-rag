import Request from "@/toolkit/request.js";
import Authorization from "@/toolkit/authorization.js";

class AIModelApi extends Request {


  async addModel(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/addModel", payload, headers);
  }

  async listMyAIModel(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/listMyAIModel", payload, headers);
  }


  async updateModel(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/updateModel", payload, headers);
  }

  async deleteModel(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/deleteModel", payload, headers);
  }
  //根据具体的modelId获取它的routeType
  async getRouteType(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/getRouteType", payload, headers);
  }

  //设置线路的type
  async setAIModelRoute(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/setAIModelRoute", payload, headers);
  }
  //把原来的线路unset
  async unsetAIModelRoute(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/unsetAIModelRoute", payload, headers);
  }
}

export default new AIModelApi("/fountainbase");