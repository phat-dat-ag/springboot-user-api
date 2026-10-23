package com.japaneselearning.user.service.impl;

import com.japaneselearning.user.dto.UserDTO;
import com.japaneselearning.user.dto.response.PageResponse;
import com.japaneselearning.user.entity.User;
import com.japaneselearning.user.repository.UserRepository;
import com.japaneselearning.user.service.UserManagementService;
import com.japaneselearning.user.specification.UserSpecification;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;

    @Override
    public PageResponse<UserDTO> getAllUsers(
            int page,
            int size,
            boolean isActivated,
            String usernamePattern,
            String emailPattern
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        Specification<@NonNull User> specification = Specification
                .where(UserSpecification.hasActivated(isActivated))
                .and(UserSpecification.usernameContains(usernamePattern))
                .and(UserSpecification.emailContains(emailPattern));

        Page<@NonNull UserDTO> userPage = userRepository
                .findAll(specification, pageable)
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.isActivated(),
                        user.getCreatedAt(),
                        user.getUpdatedAt()
                ));

        return PageResponse.from(userPage);
    }
}