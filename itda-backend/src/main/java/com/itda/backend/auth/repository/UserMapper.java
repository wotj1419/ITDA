package com.itda.backend.auth.repository;

import com.itda.backend.auth.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 사용자 MyBatis Mapper 인터페이스
 */
@Mapper
public interface UserMapper {

    /**
     * ID로 사용자 조회
     */
    Optional<User> findById(@Param("id") Long id);

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * 사용자 등록
     */
    void insertUser(User user);

    /**
     * 프로필 정보 업데이트
     */
    int updateProfile(@Param("id") Long id,
                      @Param("name") String name,
                      @Param("profileImageUrl") String profileImageUrl);

    /**
     * 비밀번호 변경
     */
    void updatePassword(@Param("id") Long id,
                        @Param("passwordHash") String passwordHash);
}
