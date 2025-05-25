import Request from "@/toolkit/request.js";
import Authorization from "@/toolkit/authorization.js";

class ChatBotApi extends Request {
  complete(params = {}, options = {}, token,userName) {
    const headers = {
      token: token,
      userName: userName,
    };

    return this.sse("/api/ai/chat/userChat", {
      ...params
    }, headers, options);
  }

  async newSession(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/initSession", payload, headers);
  }

  
  async hasMoreData(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/hasMoreData", payload, headers);
  }

  async stopChat(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/stopChat", payload, headers);
  }
}
export default new ChatBotApi("/fountainbase");