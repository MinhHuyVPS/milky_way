package milky_way.mapper;

import milky_way.dto.request.UserRequest;
import milky_way.dto.request.UserUpdateRequest;
import milky_way.dto.response.AuthResponse;
import milky_way.dto.response.UserResponse;
import milky_way.dto.response.UserUpdateResponse;
import milky_way.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper  INSTANCE = Mappers.getMapper(UserMapper.class);
    User toUser(UserRequest userRequest);
    User toAuth(String authResponse);
    AuthResponse toAuthResponse(User user);
    UserResponse toUserResponse(User user);
}
