package com.proceed.swhackathon.service;

import com.proceed.swhackathon.config.security.jwt.TokenProvider;
import com.proceed.swhackathon.dto.ResponseDTO;
import com.proceed.swhackathon.dto.user.UserDTO;
import com.proceed.swhackathon.dto.user.UserTokenDTO;
import com.proceed.swhackathon.exception.store.StoreNotFoundException;
import com.proceed.swhackathon.exception.user.*;
import com.proceed.swhackathon.model.Role;
import com.proceed.swhackathon.model.Store;
import com.proceed.swhackathon.model.User;
import com.proceed.swhackathon.repository.StoreRepository;
import com.proceed.swhackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static com.proceed.swhackathon.dto.user.UserDTO.entityToDTO;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    public User create(final User userEntity){
        if(userEntity == null || userEntity.getPlatformId() == null || userEntity.getPhoneNumber() == null){
            throw new UserInformationEmptyException();
        }

        if(userRepository.existsByPlatformTypeAndPlatformIdAndPhoneNumber(
                userEntity.getPlatformType(),
                userEntity.getPlatformId(),
                userEntity.getPhoneNumber())){
            throw new UserDuplicatedException(); // 유저 중복 Exception
        }

        return userRepository.save(userEntity);
    }

    public ResponseDTO<?> findById(final String userId){
        User user = userRepository.findById(userId).orElseThrow(() -> {
            throw new UserNotFoundException();
        });

        Map<String, Object> m = new HashMap<>();

//        m.put("user_id", user.getId());
        m.put("username", user.getUsername());
//        m.put("platformId", user.getPlatformId());
//        m.put("platformType", user.getPlatformType());
        m.put("email", user.getEmail());
        m.put("role", user.getRole());
        m.put("phoneNumber", user.getPhoneNumber());


        if(user.getRole() == Role.BOSS){
            Store store = storeRepository.findByUser(userId).orElseThrow(() -> {
                throw new StoreNotFoundException();
            });
            m.put("storeId", store.getId());
        }

        return new ResponseDTO<>(HttpStatus.OK.value(), m);
    }

    @Transactional
    public UserDTO getByCredentials(final String platformType, final String platformId){
        User originalUser = userRepository.findByPlatformIdAndAndPlatformType(platformId, platformType)
                .orElseThrow(()->{
                    throw new UserNotFoundException();
                });
        UserDTO userDTO = UserDTO.entityToDTO(originalUser);
        final String token = tokenProvider.create(originalUser);

        originalUser.setToken(token);

        userDTO.setToken(token);
        return userDTO;
    }

    // 가장 최근에 재발급받은 토큰이였을 경우, 재발급을 해주고 그렇지 않은경우는 에러를 발생시킨다.
    @Transactional
    public UserTokenDTO refreshToken(String token){
        User user = userRepository.findByToken(token).orElseThrow(() -> {
            throw new UserTokenNotFoundException();
        });
        token = tokenProvider.create(user); // 토큰 재발급
        user.setToken(token);

        return UserTokenDTO.builder()
                .token(token)
                .build();
    }
}
