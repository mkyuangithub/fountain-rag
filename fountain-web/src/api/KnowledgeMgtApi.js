import Request from "@/toolkit/request.js";
import Authorization from "@/toolkit/authorization.js";

class KnowledgeMgtApi extends Request {


  async addRepo(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/add", payload, headers);
  }

  async listUserRepo(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/list", payload, headers);
  }
  
  async removeRepoById(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
      knowledgeRepoId: payload.knowledgeRepoId,
    };
    return this.post("/api/knowledge/delete", payload, headers);
  }
  async saveMajorPrompt(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/saveMajorPrompt", payload, headers);
  }
  
  async labelRepo(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/labelRepo", payload, headers);
  }

  async makeIndex(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/makeIndex", payload, headers);
  }

  async getDetailKnowledgeItem(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/getDetailKnowledgeItem", payload, headers);
  }

  async updateDetailKnowledgeItem(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/updateDetailKnowledgeItem", payload, headers);
  }

  async deleteDetailKnowledgeItem(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/deleteDetailKnowledgeItem", payload, headers);
  }
  
  async queryElasticStatus(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/settings/queryElasticStatus", payload, headers);
  }

  async syncToES(payload) {
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/syncToES", payload, headers);
  }

  async setSplitType(payload){
    const headers = {
      userName: payload.userName,
      token: payload.token,
    };
    return this.post("/api/knowledge/setSplitType", payload, headers);
  }
}



export default new KnowledgeMgtApi("/fountainbase");