package milky_way.mapper;

import milky_way.dto.request.UserUpdateRequest;
import milky_way.dto.response.UserUpdateResponse;
import milky_way.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper  INSTANCE = Mappers.getMapper(UserMapper.class);
    User toUser(UserUpdateRequest userUpdateRequest);
    UserUpdateResponse toUserResponse(User user);
}
