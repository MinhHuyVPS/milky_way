import { useEffect, useState } from "react";
import { fetchUsers } from "../api/userApi";

interface User {
  id: string;
  firstname: string;
  lastname: string;
  email: string;
}

const UserList = () => {
  const [users, setUsers] = useState<User[]>([]);

  useEffect(() => {
    fetchUsers().then((data) => setUsers(data));
  }, []);

  return (
    <div>
      <h2>Danh sách Người Dùng</h2>
      <ul>
        {users.length > 0 ? (
          users.map((user) => (
            <li key={user.id}>
              {user.firstname} {user.lastname} - {user.email}
            </li>
          ))
        ) : (
          <p>Không có dữ liệu</p>
        )}
      </ul>
    </div>
  );
};

export default UserList;
