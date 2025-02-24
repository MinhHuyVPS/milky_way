import axios from "axios";

export const registerUser = async (data: { email: string; password: string }) => {
  return axios.post("http://localhost:8000/api/register", data);
};

