package kr.kro.tripsketch.controllers

import kr.kro.tripsketch.dto.AdditionalUserInfo
import kr.kro.tripsketch.dto.UserLoginDto
import kr.kro.tripsketch.dto.UserRegistrationDto
import kr.kro.tripsketch.dto.UserUpdateDto
import kr.kro.tripsketch.services.KakaoOAuthService
import kr.kro.tripsketch.services.UserService
import kr.kro.tripsketch.services.JwtService
import kr.kro.tripsketch.services.NickNameService
import kr.kro.tripsketch.domain.User

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/oauth")
class UserController(
    private val kakaoOAuthService: KakaoOAuthService,
    private val userService: UserService,
    private val jwtService: JwtService,
    private val nicknameService: NickNameService,
) {


    //카카오 Oauth2.0을 이용한 사용자 로그인/회원가입 콜백함수
    @GetMapping("/kakao/callback")
    fun kakaoCallback(@RequestParam code: String): ResponseEntity<Any> {
        val accessToken = kakaoOAuthService.getKakaoAccessToken(code)
        val userInfo = kakaoOAuthService.getUserInfo(accessToken)

        if (userInfo == null) {
            return ResponseEntity.status(400).body("유효하지 않은 요청입니다.")
        }

        val kakaoAccountInfo = userInfo["kakao_account"] as? Map<*, *>
        val email = kakaoAccountInfo?.get("email")?.toString()
        if (email == null) {
            return ResponseEntity.status(400).body("이메일 정보가 없습니다.")
        }

        var user = userService.findUserByEmail(email)
        if (user == null) {
            // 회원 가입을 위한 추가 정보가 없는 경우, 임의의 값을 설정합니다.
            val additionalUserInfo = AdditionalUserInfo(
                nickname = nicknameService.generateRandomNickname(), // 임의의 닉네임
                profileImageUrl = "https://example.com/default-profile-image.png", // 기본 프로필 이미지 URL
                introduction = "Nice to meet you!" // 기본 소개 문구
            )
            val userRegistrationDto = UserRegistrationDto(
                email,
                additionalUserInfo.nickname,
                additionalUserInfo.profileImageUrl,
                additionalUserInfo.introduction
            )
            user = userService.registerUser(userRegistrationDto)
        }

        val jwt = jwtService.createToken(user)
        val response = mapOf("token" to jwt)
        return ResponseEntity.ok(response)
    }

    // // 사용자 정보를 이메일로 조회하는 메소드
    // @GetMapping("/user")
    // fun getUserByEmail(@RequestParam email: String): ResponseEntity<Any> {
    //     val user = userService.findUserByEmail(email)
    //     if (user != null) {
    //         return ResponseEntity.ok(user)
    //     } else {
    //         return ResponseEntity.status(404).body("유저 정보가 없습니다.")
    //     }
    // }

    // 토큰값으로 사용자를 조회하는 메소드
    @GetMapping("/user")
    fun getUser(@RequestHeader("Authorization") token: String): ResponseEntity<Any> {
        val actualToken = token.removePrefix("Bearer ").trim() // "Bearer " 제거
        if (!jwtService.validateToken(actualToken)) { // 토큰 유효성 검증
            return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.")
        }
        val email = jwtService.getEmailFromToken(actualToken) // 토큰에서 이메일 추출
        val user = userService.findUserByEmail(email) ?: return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.")
        return ResponseEntity.ok(user)
    }

    // 사용자 정보를 업데이트하는 메소드
    @PatchMapping("/user")
    fun updateUser(@RequestParam email: String, @RequestBody userUpdateDto: UserUpdateDto): ResponseEntity<Any> {
        try {
            val updatedUser = userService.updateUser(email, userUpdateDto)
            return ResponseEntity.ok(updatedUser)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.status(400).body(e.message)
        }
    }

    // 전체 사용자를 조회하는 메소드
    @GetMapping("/users")
    fun getAllUsers(): ResponseEntity<List<User>> {
        val users = userService.getAllUsers()
        return ResponseEntity.ok(users)
    }


}