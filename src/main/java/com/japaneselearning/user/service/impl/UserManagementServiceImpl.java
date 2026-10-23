package com.japaneselearning.user.service.impl;

import com.japaneselearning.user.common.exception.BusinessException;
import com.japaneselearning.user.common.exception.ErrorCode;
import com.japaneselearning.user.dto.UserDTO;
import com.japaneselearning.user.dto.response.PageResponse;
import com.japaneselearning.user.dto.response.UserResponse;
import com.japaneselearning.user.entity.User;
import com.japaneselearning.user.repository.UserRepository;
import com.japaneselearning.user.service.UserManagementService;
import com.japaneselearning.user.specification.UserSpecification;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
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

        log.info(
                "Got all users successfully, "
                        + "condition: isActivated={}, usernamePattern={}, emailPattern={}"
                        + "result: contentCount={}, page={}, size={}, "
                        + "totalElements={}, totalPages={}",
                isActivated, usernamePattern, emailPattern,
                userPage.getContent().size(), userPage.getNumber(), userPage.getSize(),
                userPage.getTotalElements(), userPage.getTotalPages()
        );

        return PageResponse.from(userPage);
    }

    private User getUserByUUID(UUID uuid) {
        Optional<User> optionalUser = userRepository.findById(uuid);

        if (optionalUser.isEmpty()) {
            log.warn("User not found, uuid={}", uuid);

            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return optionalUser.get();
    }

    @Override
    public UserResponse getUser(UUID uuid) {
        User user = getUserByUUID(uuid);

        log.info("Got user successfully, uuid={}", user.getId());

        return new UserResponse(new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isActivated(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        ));
    }

    private void validateActivationStatus(boolean currentStatus, boolean newStatus) {
        if (currentStatus == newStatus) {
            log.warn(
                    "Invalid activation status: currentStatus={}, newStatus={}",
                    currentStatus, newStatus
            );

            if (currentStatus) {
                throw new BusinessException(ErrorCode.USER_MANAGEMENT_ACTIVATION_ACTIVATED);
            } else {
                throw new BusinessException(ErrorCode.USER_MANAGEMENT_ACTIVATION_DEACTIVATED);
            }
        }
    }

    @Override
    public void updateUserActivation(UUID uuid, boolean newActivationStatus) {
        User user = getUserByUUID(uuid);

        validateActivationStatus(user.isActivated(), newActivationStatus);

        user.setActivated(newActivationStatus);

        userRepository.save(user);

        log.info(
                "Updated new activation status successfully, uuid={}, isActivated={}",
                uuid, newActivationStatus
        );
    }
}