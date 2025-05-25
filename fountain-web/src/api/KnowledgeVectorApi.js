import Request from "@/toolkit/request.js";
import Authorization from "@/toolkit/authorization.js";

class KnowledgeVectorApi extends Request {


  async uploadDocIntoKnowledge(form, payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.rawPost("/api/knowledge/uploadDoc", form, headers);
  }

  async executeEmbedding(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/executeEmbedding", payload, headers);
  }

  async listDetail(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/detail/list", payload, headers);
  }

  async getKnowledgeInfo(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/detail/getKnowledgeInfo", payload, headers);
  }
  
  async getImportStatus(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/detail/getImportStatus", payload, headers);
  }

  async stopUploadProcess(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/detail/stopUploadProcess", payload, headers);
  }
  async addKnowledgeDetail(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/detail/addKnowledgeDetail", payload, headers);
  }
}


export default new KnowledgeVectorApi("/fountainbase");