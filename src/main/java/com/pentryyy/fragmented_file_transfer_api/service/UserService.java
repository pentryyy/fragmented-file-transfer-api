package com.pentryyy.fragmented_file_transfer_api.service;

import com.pentryyy.fragmented_file_transfer_api.repository.UserRepository;
import com.pentryyy.fragmented_file_transfer_api.dto.UserUpdateRequest;
import com.pentryyy.fragmented_file_transfer_api.enumeration.RoleType;
import com.pentryyy.fragmented_file_transfer_api.exception.EmailAlreadyExistsException;
import com.pentryyy.fragmented_file_transfer_api.exception.RoleDoesNotExistException;
import com.pentryyy.fragmented_file_transfer_api.exception.UserAlreadyDisabledException;
import com.pentryyy.fragmented_file_transfer_api.exception.UserAlreadyEnabledException;
import com.pentryyy.fragmented_file_transfer_api.exception.UserDoesNotExistException;
import com.pentryyy.fragmented_file_transfer_api.exception.UsernameAlreadyExistsException;
import com.pentryyy.fragmented_file_transfer_api.model.User;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service; 
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User findById(Long id) {
        return userRepository.findById(id)
                             .orElseThrow(() -> new UserDoesNotExistException(id));
    }

    public void disableUser(Long id) {
        User user = findById(id);

        if (!user.getIsEnabled()) {
            throw new UserAlreadyDisabledException();
        }

        user.setIsEnabled(false);
        userRepository.save(user);
    }

    public void enableUser(Long id) {
        User user = findById(id);

        if (user.getIsEnabled()) {
            throw new UserAlreadyEnabledException();
        }

        user.setIsEnabled(true);
        userRepository.save(user);
    }

    public void changeRole(Long id, String rolename){
        User user = findById(id);

        if (!EnumUtils.isValidEnum(RoleType.class, rolename)) {
            throw new RoleDoesNotExistException(rolename);
        }

        user.setRole(RoleType.valueOf(rolename));
        userRepository.save(user);
    }

    public void updateUser(
        Long id, 
        UserUpdateRequest request) {
        
        User user = findById(id);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        userRepository.save(user);
    }

    public void changePassword(
        Long id, 
        String encryptedPassword) {
        
        User user = findById(id);

        user.setPassword(encryptedPassword);
        userRepository.save(user);
    }

    public User saveNewUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new UsernameAlreadyExistsException(user.getUsername());
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException(user.getEmail());
        }

        return userRepository.save(user);
    }
    
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                             .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
    }

    public UserDetailsService userDetailsService() {
        return this::getByEmail;
    }

    public Page<User> getAllUsers(
        int page, 
        int limit,
        String sortBy, 
        String sortOrder) {
        
        Sort sort = sortOrder.equalsIgnoreCase(Sort.Direction.ASC.name())
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
        return userRepository.findAll(PageRequest.of(page, limit, sort));
    }
}