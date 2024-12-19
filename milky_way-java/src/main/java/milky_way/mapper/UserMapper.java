package milky_way.mapper;

import milky_way.dto.request.user.UserRequest;
import milky_way.dto.response.auth.LinkGoogleResponse;
import milky_way.dto.response.auth.LoginResponse;
import milky_way.dto.response.user.UserChangeStatusResponse;
import milky_way.dto.response.user.UserResponse;
import milky_way.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper  INSTANCE = Mappers.getMapper(UserMapper.class);
    User toUser(UserRequest userRequest);
    User toAuth(String authResponse);
    LinkGoogleResponse toAuthResponse(User user);
    UserResponse toUserResponse(User user);
    LoginResponse toLoginResponse(User user);
    UserChangeStatusResponse toChangeStatus(User user);
}
