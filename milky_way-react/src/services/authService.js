import apiClient from "../api/axios";
 const registerUser = async (userData) =>{
    try{
        const response = await apiClient.post('/register', userData);
        return response.data;
    }catch(error){
        console.error('Error registering user:', error);
        throw error;
    };
 }

 export{registerUser}


