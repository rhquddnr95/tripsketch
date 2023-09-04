package kr.kro.tripsketch.services

import kr.kro.tripsketch.domain.Comment
import kr.kro.tripsketch.dto.*
import kr.kro.tripsketch.exceptions.BadRequestException
import kr.kro.tripsketch.exceptions.ForbiddenException
import kr.kro.tripsketch.repositories.CommentRepository
import kr.kro.tripsketch.repositories.TripRepository
import kr.kro.tripsketch.repositories.UserRepository
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val tripRepository: TripRepository
) {

    fun getAllComments(pageable: Pageable): Page<CommentDto> {
        return commentRepository.findAll(pageable).map { fromComment(it, userRepository) }
    }

    fun getCommentByTripId(tripId: String): List<CommentDto> {
        val comments = commentRepository.findAllByTripId(tripId)
        return comments.map { fromComment(it, userRepository) }
    }

    /** 로그인 한 유저가 좋아요가 있는 댓글 조회 */
    fun getIsLikedByTokenForTrip(email: String, tripId: String): List<CommentDto> {
        val updatedComments = isLikedByTokenForComments(email, tripId)

        return updatedComments.map { fromComment(it, userRepository) }
    }


    private fun isLikedByTokenForComments(userEmail: String, tripId: String): List<Comment> {
        val comments = commentRepository.findAllByTripId(tripId)

        return comments.map { comment ->
            val isLiked = comment.likedBy.contains(userEmail)
            val updatedComment = comment.copy(isLiked = isLiked)

            val updatedChildren = comment.children.map { child ->
                child.copy(isLiked = child.likedBy.contains(userEmail))
            }.toMutableList()

            updatedComment.copy(children = updatedChildren)
        }
    }

    // 트립아이디 조회 하여 널이 아닐경우에만 글을 쓰도록
    fun createComment(email: String, commentCreateDto: CommentCreateDto): CommentDto {

        val findTrip = tripRepository.findByIdAndIsHiddenIsFalse(commentCreateDto.tripId)
            ?: throw IllegalArgumentException("해당 게시글이 존재하지 않습니다.")
        val commenter = userRepository.findByEmail(email) ?: throw IllegalArgumentException("해당 이메일의 사용자 존재하지 않습니다.")
        val comment = Comment(
            userEmail = email,
            tripId = commentCreateDto.tripId,
            content = commentCreateDto.content,
        )
        val createdComment = commentRepository.save(comment)

        val tripEmail = findTrip.email
        // 알림 적용
        if (tripEmail != email)
            notificationService.sendPushNotification(
                listOf(tripEmail),
                "새로운 여행의 시작, 트립스케치",
                "${commenter.nickname} 님이 댓글을 남겼습니다. ",
                nickname = commenter.nickname,
                profileUrl = commenter.profileImageUrl,
                tripId = comment.tripId,
                commentId = comment.id
            )
        return fromComment(createdComment, userRepository)
    }

    fun createChildrenComment(
        email: String,
        parentId: String,
        commentChildrenCreateDto: CommentChildrenCreateDto
    ): CommentDto {

        val findTrip = tripRepository.findByIdAndIsHiddenIsFalse(commentChildrenCreateDto.tripId)
            ?: throw IllegalArgumentException("해당 게시글이 존재하지 않습니다.")
        val commenter = userRepository.findByEmail(email) ?: throw IllegalArgumentException("해당 이메일의 사용자 존재하지 않습니다.")

        val parentComment: Comment = parentId.let {
            commentRepository.findById(it).orElse(null)
        }
            ?: // 적절한 에러 처리 로직 추가
            throw IllegalArgumentException("해당 parentId 댓글은 존재하지 않습니다.")

        val mentionedUser =
            userRepository.findByNickname(commentChildrenCreateDto.replyToNickname) ?: // 적절한 에러 처리 로직 추가
            throw IllegalArgumentException("해당 이메일의 언급 된 사용자 존재하지 않습니다.")

        val childComment = Comment(
            id = ObjectId().toString(), // 새로운 ObjectId 생성
            userEmail = email,
            tripId = commentChildrenCreateDto.tripId,
            parentId = parentId,
            content = commentChildrenCreateDto.content,
            replyToEmail = mentionedUser.email,
        )

        parentComment.children.add(childComment)
        val createdComment = commentRepository.save(parentComment)

        val tripEmail = findTrip.email
        // 알림 적용
        val notificationRecipients = mutableListOf<String>()

        if (tripEmail != email) {
            notificationRecipients.add(tripEmail)
        }

        if (parentComment.userEmail != email) {
            notificationRecipients.add(parentComment.userEmail)
        }

        if (mentionedUser.email != email) {
            notificationRecipients.add(mentionedUser.email)
        }

        println("$notificationRecipients : notificationRecipients")
        if (notificationRecipients.isNotEmpty()) {
            notificationService.sendPushNotification(
                notificationRecipients,
                "새로운 여행의 시작, 트립스케치",
                "${commenter.nickname} 님이 댓글을 남겼습니다. ",
                nickname = commenter.nickname,
                profileUrl = commenter.profileImageUrl,
                tripId = childComment.tripId,
                parentId = childComment.parentId,
                commentId = childComment.id
            )
        }




        return fromComment(createdComment, userRepository)
    }

    fun updateComment(email: String, id: String, commentUpdateDto: CommentUpdateDto): CommentDto {
        val comment =
            commentRepository.findById(id).orElse(null) ?: throw IllegalArgumentException("해당 id 댓글은 존재하지 않습니다.")
        if (comment.userEmail != email) {
            throw ForbiddenException("해당 사용자만 접근 가능합니다.")
        }
        val updatedTime = LocalDateTime.now()
        val updatedComment = comment.copy(
            content = commentUpdateDto.content ?: comment.content,
            updatedAt = updatedTime
        )

        val savedComment = commentRepository.save(updatedComment)

        return fromComment(savedComment, userRepository)
    }

    fun updateChildrenComment(
        email: String,
        parentId: String,
        id: String,
        commentUpdateDto: CommentUpdateDto
    ): CommentDto {
        val parentComment = commentRepository.findById(parentId).orElse(null)
            ?: throw IllegalArgumentException("해당 parentId 댓글은 존재하지 않습니다.")

        val childCommentIndex = parentComment.children.indexOfFirst { it.id == id }
        if (childCommentIndex == -1) {
            throw IllegalArgumentException("해당 id에 대응하는 댓글이 children 존재하지 않습니다.")
        }

        if (parentComment.children[childCommentIndex].userEmail != email) {
            throw ForbiddenException("해당 사용자만 접근 가능합니다.")
        }

        val updatedTime = LocalDateTime.now()
        val updatedChildComment = parentComment.children[childCommentIndex].copy(
            content = commentUpdateDto.content ?: parentComment.children[childCommentIndex].content,
            updatedAt = updatedTime
        )

        parentComment.children[childCommentIndex] = updatedChildComment
        val savedParentComment = commentRepository.save(parentComment)

        return fromComment(savedParentComment, userRepository)
    }

    fun deleteComment(email: String, id: String) {
        val comment = commentRepository.findById(id).orElse(null)
            ?: throw IllegalArgumentException("해당 id 댓글은 존재하지 않습니다.")
        if (comment.isDeleted) {
            throw BadRequestException("이미 삭제 된 댓글 입니다.")
        }
        if (comment.userEmail != email) {
            throw ForbiddenException("해당 사용자만 접근 가능합니다.")
        }
        // Soft delete 처리
        val deletedComment = comment.copy(
            content = "삭제 된 댓글입니다.",
            isDeleted = true
        )
        commentRepository.save(deletedComment)
    }

    fun deleteChildrenComment(email: String, parentId: String, id: String) {
        val parentComment = commentRepository.findById(parentId).orElse(null)
            ?: throw IllegalArgumentException("해당 parentId 댓글은 존재하지 않습니다.")

        val childCommentIndex = parentComment.children.indexOfFirst { it.id == id }
        if (childCommentIndex == -1) {
            throw IllegalArgumentException("해당 id에 대응하는 댓글이 children에 존재하지 않습니다.")
        }

        if (parentComment.children[childCommentIndex].isDeleted) {
            throw BadRequestException("이미 삭제 된 댓글 입니다.")
        }

        if (parentComment.children[childCommentIndex].userEmail != email) {
            throw ForbiddenException("해당 사용자만 접근 가능합니다.")
        }

        // Soft delete 처리
        val deletedChildComment =
            parentComment.children[childCommentIndex].copy(content = "삭제 된 댓글입니다.", isDeleted = true)
        parentComment.children[childCommentIndex] = deletedChildComment
        commentRepository.save(parentComment)
    }

    fun toggleLikeComment(email: String, id: String): CommentDto {

        val commenter = userRepository.findByEmail(email) ?: throw IllegalArgumentException("해당 이메일의 사용자 존재하지 않습니다.")

        val comment = commentRepository.findById(id).orElse(null)
            ?: throw IllegalArgumentException("해당 id 댓글은 존재하지 않습니다.")
        if (comment.likedBy.contains(email)) {
            comment.likedBy.remove(email) // 이미 좋아요를 누른 경우 좋아요 취소
            comment.numberOfLikes -= 1
        } else {
            comment.likedBy.add(email) // 좋아요 추가
            comment.numberOfLikes += 1

            // 자신의 댓글에 좋아요를 남기지 않았을 경우
            if (email != comment.userEmail) {
                // 알림 적용
                notificationService.sendPushNotification(
                    listOf(comment.userEmail),
                    "새로운 여행의 시작, 트립스케치",
                    "${commenter.nickname} 님이 댓글에 좋아요.",
                    nickname = commenter.nickname,
                    profileUrl = commenter.profileImageUrl,
                    tripId = comment.tripId,
                    commentId = comment.id
                )
            }
        }

        val savedComment = commentRepository.save(comment)
        return fromComment(savedComment, userRepository)
    }

    fun toggleLikeChildrenComment(email: String, parentId: String, id: String): CommentDto {

        val parentComment = commentRepository.findById(parentId).orElse(null)
            ?: throw IllegalArgumentException("해당 parentId 댓글은 존재하지 않습니다.")
        val commenter = userRepository.findByEmail(email) ?: throw IllegalArgumentException("해당 이메일의 사용자 존재하지 않습니다.")

        val childCommentIndex = parentComment.children.indexOfFirst { it.id == id }
        if (childCommentIndex == -1) {
            throw IllegalArgumentException("해당 id에 대응하는 댓글이 children에 존재하지 않습니다.")
        }

        val childComment = parentComment.children[childCommentIndex]
        if (childComment.likedBy.contains(email)) {
            childComment.likedBy.remove(email) // 이미 좋아요를 누른 경우 좋아요 취소
            childComment.numberOfLikes -= 1
        } else {
            childComment.likedBy.add(email) // 좋아요 추가
            childComment.numberOfLikes += 1
            // 자신의 댓글에 좋아요를 남기지 않았을 경우
            if (email != childComment.userEmail) {
                // 알림 적용
                notificationService.sendPushNotification(
                    listOf(childComment.userEmail),
                    "새로운 여행의 시작, 트립스케치",
                    "${commenter.nickname} 님이 님이 댓글에 좋아요.",
                    nickname = commenter.nickname,
                    profileUrl = commenter.profileImageUrl,
                    tripId = childComment.tripId,
                    parentId = childComment.parentId,
                    commentId = childComment.id
                )
            }
        }

        val savedParentComment = commentRepository.save(parentComment)
        return fromComment(savedParentComment, userRepository)
    }

    companion object {
        fun fromComment(comment: Comment, userRepository: UserRepository): CommentDto {
            val commenter = userRepository.findByEmail(comment.userEmail)
            val mentionedUser = comment.replyToEmail?.let { userRepository.findByEmail(it) }

            val commenterProfile = commenter?.let {
                UserProfileDto(
                    email = it.email,
                    nickname = it.nickname,
                    introduction = it.introduction,
                    profileImageUrl = it.profileImageUrl
                )
            }

            val mentionedUserNickname = mentionedUser?.nickname

            return CommentDto(
                id = comment.id,
                userNickName = commenterProfile?.nickname ?: "", // 사용자가 없을 경우 대비
                userProfileUrl = commenterProfile?.profileImageUrl ?: "", // 사용자가 없을 경우 대비
                tripId = comment.tripId,
                parentId = comment.parentId,
                content = comment.content,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
                replyToNickname = mentionedUserNickname,
                isDeleted = comment.isDeleted,
                isLiked = comment.isLiked,
                numberOfLikes = comment.numberOfLikes,
                children = comment.children.map { fromComment(it, userRepository) }.toMutableList(),
            )
        }
    }
}