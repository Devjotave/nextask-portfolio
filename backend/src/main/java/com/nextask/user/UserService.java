package com.nextask.user;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nextask.api.PageResponse;
import com.nextask.entity.User;
import com.nextask.user.api.CreateUserRequest;
import com.nextask.user.api.UpdateUserRequest;
import com.nextask.user.api.UserResponse;
import com.nextask.user.error.EmailAlreadyInUseException;
import com.nextask.user.error.UserNotFoundException;
import com.nextask.user.error.UserHasWorkspaceMembershipsException;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        rejectDuplicateEmail(normalizedEmail);

        User user = new User(
                request.name(),
                normalizedEmail,
                passwordEncoder.encode(request.password()));

        try {
            return toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyInUseException();
        }
    }

    public UserResponse findById(Long id) {
        return toResponse(findUser(id));
    }

    public PageResponse<UserResponse> findAll(int page, int size) {
        Page<UserResponse> users = userRepository.findAll(PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.ASC, "id")))
                .map(UserService::toResponse);

        return PageResponse.from(users);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new EmailAlreadyInUseException();
        }

        user.rename(request.name());
        user.changeEmail(normalizedEmail);

        try {
            return toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyInUseException();
        }
    }

    @Transactional
    public void delete(Long id) {
        User user = findUser(id);

        try {
            userRepository.delete(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new UserHasWorkspaceMembershipsException(id);
        }
    }

    private void rejectDuplicateEmail(String normalizedEmail) {
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyInUseException();
        }
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
