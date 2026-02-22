package com.whatsapp.user.infrastructure.persistence.repository;

import com.whatsapp.user.domain.model.User;
import com.whatsapp.user.domain.model.vo.Email;
import com.whatsapp.user.domain.model.vo.PhoneNumber;
import com.whatsapp.user.domain.model.vo.UserId;
import com.whatsapp.user.domain.repository.UserRepository;
import com.whatsapp.user.infrastructure.persistence.entity.UserEntity;
import com.whatsapp.user.infrastructure.persistence.mapper.UserEntityMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * User Repository Implementation
 *
 * Infrastructure implementation of UserRepository.
 * Bridges domain layer with persistence layer.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserEntityMapper entityMapper;

    @Override
    public User save(User user) {
        log.debug("Saving user: {}", user.getId());

        UserEntity entity = entityMapper.toEntity(user);
        UserEntity savedEntity = jpaRepository.save(entity);

        log.debug("User saved successfully: {}", savedEntity.getId());
        return entityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        log.debug("Finding user by ID: {}", userId.getValue());

        return jpaRepository.findById(userId.getValue())
                .map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);

        return jpaRepository.findByUsername(username)
                .map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        log.debug("Finding user by email: {}", email.getValue());

        return jpaRepository.findByEmail(email.getValue())
                .map(entityMapper::toDomain);
    }

    @Override
    public Optional<User> findByPhoneNumber(PhoneNumber phoneNumber) {
        log.debug("Finding user by phone number: {}", phoneNumber.getValue());

        return jpaRepository.findByPhoneNumber(phoneNumber.getValue())
                .map(entityMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.getValue());
    }

    @Override
    public boolean existsByPhoneNumber(PhoneNumber phoneNumber) {
        return jpaRepository.existsByPhoneNumber(phoneNumber.getValue());
    }

    @Override
    public List<User> searchByUsername(String usernamePattern, int limit) {
        log.debug("Searching users by pattern: {}, limit: {}", usernamePattern, limit);

        return jpaRepository.searchByUsername(usernamePattern).stream()
                .limit(limit)
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findAllActive() {
        log.debug("Finding all active users");

        return jpaRepository.findByActiveTrue().stream()
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UserId userId) {
        log.info("Deleting user: {}", userId.getValue());

        jpaRepository.deleteById(userId.getValue());

        log.info("User deleted: {}", userId.getValue());
    }
}