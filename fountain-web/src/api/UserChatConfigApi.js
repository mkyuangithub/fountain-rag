import Request from "@/toolkit/request.js";
import Authorization from "@/toolkit/authorization.js";

class UserChatConfigApi extends Request {

  async initChatConfig(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/initConfig", payload, headers);
  }

  async getChatConfigListByUserName(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/getChatConfigListByUserName", payload, headers);
  }

  async getChatConfigMainId(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/getChatConfigMainId", payload, headers);
  }

  async getAllKnowledgeRepo(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/getAllKnowledgeRepo", payload, headers);
  }

  async getChatConfigByMainId(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/getChatConfigByMainId", payload, headers);
  }

  async saveConfig(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/saveConfig", payload, headers);
  }

  async updateConfig(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/updateConfig", payload, headers);
  }

  async deleteConfig(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/deleteConfig", payload, headers);
  }

  async backupAllConfig(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/backupAllConfig", payload, headers);
  }

  async getBackupList(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/getBackupList", payload, headers);
  }


  async downloadBackupFile(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    // 添加 options 参数，指定 responseType 为 blob
    return this.post("/api/ai/chat/downloadBackupFile", payload, headers, {
      responseType: 'blob'
    });
  }

  async removeBackupFile(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/removeBackupFile", payload, headers);
  }

  async restoreByBackupFile(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/ai/chat/restoreByBackupFile", payload, headers);
  }

}
export default new UserChatConfigApi("/fountainbase");