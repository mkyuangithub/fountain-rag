import Request from "@/toolkit/request.js";
import Authorization from "@/toolkit/authorization.js";

class PictureApi extends Request {

  
  async uploadPic(form, payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.rawPost("/api/tool/upload", form, headers);
  }

  async removeBG(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/demo/pictureAgent/removeBG", payload, headers);
  }

  async removeWatermark(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/demo/pictureAgent/removeWatermark", payload, headers);
  }

}
export default new PictureApi("/fountainbase");