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

        log.atDebug()
                .addKeyValue("event", "users.list.completed")
                .addKeyValue("resultCount", userPage.getNumberOfElements())
                .log("User search completed");

        return PageResponse.from(userPage);
    }

    private User findUserOrThrow(UUID uuid) {
        Optional<User> optionalUser = userRepository.findById(uuid);

        if (optionalUser.isEmpty()) {
            log.debug("User lookup returned no account");

            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return optionalUser.get();
    }

    @Override
    public UserResponse getUser(UUID uuid) {
        User user = findUserOrThrow(uuid);

        log.debug("User lookup completed");

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
            log.debug("Activation update rejected: status unchanged");

            if (currentStatus) {
                throw new BusinessException(ErrorCode.USER_MANAGEMENT_ACTIVATION_ACTIVATED);
            } else {
                throw new BusinessException(ErrorCode.USER_MANAGEMENT_ACTIVATION_DEACTIVATED);
            }
        }
    }

    @Override
    @Transactional
    public void updateUserActivation(UUID uuid, boolean newActivationStatus) {
        User user = findUserOrThrow(uuid);

        validateActivationStatus(user.isActivated(), newActivationStatus);

        user.setActivated(newActivationStatus);

        userRepository.save(user);

        log.atInfo()
                .addKeyValue("event", "users.activation.updated")
                .log("User activation updated");
    }
}
