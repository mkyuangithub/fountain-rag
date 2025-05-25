import Request from "@/toolkit/request.js";
import Authorization from "@/toolkit/authorization.js";

class SystemUserMgtApi extends Request {

  async listUsers(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/user/listUsers", payload, headers);
  }
  async searchUser(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/user/searchUser", payload, headers);
  }

  async addUser(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/user/add", payload, headers);
  }

  async updateUser(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/user/updateUser", payload, headers);
  }

  async deleteUser(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/user/deleteUsers", payload, headers);
  }

}

export default new SystemUserMgtApi("/fountainbase");