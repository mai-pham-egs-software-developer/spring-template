package com.my.craft.security.service.initial;

import java.util.List;

import com.my.craft.security.service.UserAdminService;
import com.my.craft.security.service.UserNotFoundException;
import org.springframework.stereotype.Service;

import com.my.craft.security.dto.CreateUserRequest;
import com.my.craft.security.dto.UpdateUserRequest;
import com.my.craft.security.dto.UserResponse;
import com.my.craft.security.domain.User;
import com.my.craft.security.repository.UserJpaRepository;

@Service
public class DefaultUserAdminService implements UserAdminService {

    private final UserJpaRepository userRepository;

    public DefaultUserAdminService(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Override
    public UserResponse findById(String id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Override
    public UserResponse create(CreateUserRequest request) {
        User user = new User(request.id(), request.name());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public UserResponse update(String id, UpdateUserRequest request) {
        User user = findOrThrow(id);
        user.setName(request.name());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public void delete(String id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    private User findOrThrow(String id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
