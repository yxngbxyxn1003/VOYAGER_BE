package com.planty.service.chat;

import com.planty.dto.chat.ChatDto;
import com.planty.dto.chat.ChatMessageDto;
import com.planty.dto.chat.ChatRoomDto;
import com.planty.dto.chat.ParticipantDto;
import com.planty.dto.user.BlockUserDto;
import com.planty.entity.chat.Chat;
import com.planty.entity.chat.ChatMessage;
import com.planty.entity.chat.ChatUser;
import com.planty.entity.user.BlockUser;
import com.planty.entity.user.User;
import com.planty.repository.chat.ChatMessageRepository;
import com.planty.repository.chat.ChatRepository;
import com.planty.repository.chat.ChatUserRepository;
import com.planty.repository.user.BlockUserRepository;
import com.planty.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatUserRepository chatUserRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final BlockUserRepository blockUserRepository;

    // 채팅 시작
    @Transactional
    public ChatDto startChat(Integer userId, Integer sellerId) {
        Optional<Chat> existingChat = chatUserRepository.findExistingChatBetweenUsers(userId, sellerId);
        Chat chat;
        if (existingChat.isPresent()) {
            chat = existingChat.get();
        } else {
            chat = new Chat();
            chat.setCreatedAt(LocalDateTime.now());
            chat.setModifiedAt(LocalDateTime.now());
            chatRepository.save(chat);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            User seller = userRepository.findById(sellerId)
                    .orElseThrow(() -> new RuntimeException("Seller not found"));

            ChatUser cu1 = new ChatUser();
            cu1.setChat(chat);
            cu1.setUser(user);

            ChatUser cu2 = new ChatUser();
            cu2.setChat(chat);
            cu2.setUser(seller);

            chatUserRepository.save(cu1);
            chatUserRepository.save(cu2);
        }

        return new ChatDto(chat);
    }

    // 채팅 기록 조회
    public List<ChatMessageDto> getChatMessages(Long chatId) {
        return chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId).stream()
                .map(msg -> new ChatMessageDto(
                        msg.getId(),
                        msg.getChat().getId(),
                        msg.getSender().getId(),
                        msg.getContent(),
                        msg.getRead(),
                        msg.getChatImg(),
                        msg.getCreatedAt(),
                        msg.getModifiedAt()
                )).toList();
    }

    // 발신/수신 메시지 조회
    public List<ChatMessageDto> getMessagesBySender(Long chatId, Integer senderId) {
        return chatMessageRepository.findByChatIdAndSenderId(chatId, senderId).stream()
                .map(msg -> new ChatMessageDto(
                        msg.getId(),
                        msg.getChat().getId(),
                        msg.getSender().getId(),
                        msg.getContent(),
                        msg.getRead(),
                        msg.getChatImg(),
                        msg.getCreatedAt(),
                        msg.getModifiedAt()
                )).toList();
    }

    @Transactional
    public void readChat(Long chatId, Integer userId) {
        int readed = chatMessageRepository.markAsRead(chatId, userId);
        System.out.println("Updated " + readed);
    }

    @Transactional
    public ChatMessageDto sendMessage(Long chatId, Integer senderId, String content) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("채팅이 없습니다."));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다." + senderId));

        boolean isParticipant = chatUserRepository.existsByChatIdAndUserId(chatId, senderId);
        if (!isParticipant) {
            throw new RuntimeException("채팅 권한이 없는 유저입니다.");
        }

        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(content);
        message.setRead(false);
        message.setCreatedAt(LocalDateTime.now());
        message.setModifiedAt(LocalDateTime.now());

        chatMessageRepository.save(message);

        return new ChatMessageDto(
                message.getId(),
                chatId,
                senderId,
                content,
                false,
                null,
                message.getCreatedAt(),
                message.getModifiedAt()
        );
    }

    @Transactional
    public ChatMessageDto sendMessage(Long chatId, Integer senderId, String content, String file) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("채팅이 없습니다."));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다." + senderId));

        boolean isParticipant = chatUserRepository.existsByChatIdAndUserId(chatId, senderId);
        if (!isParticipant) {
            throw new RuntimeException("채팅 권한이 없는 유저입니다.");
        }

        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(content);
        message.setRead(false);
        message.setChatImg(file);
        message.setCreatedAt(LocalDateTime.now());
        message.setModifiedAt(LocalDateTime.now());

        chatMessageRepository.save(message);

        return new ChatMessageDto(
                message.getId(),
                chatId,
                senderId,
                content,
                false,
                file,
                message.getCreatedAt(),
                message.getModifiedAt()
        );
    }

    // 내가 속한 채팅방 목록 조회
    public List<ChatRoomDto> getMyChatRooms(Integer userId) {
        // 내가 참여한 ChatUser 레코드 조회
        List<ChatUser> myChatUsers = chatUserRepository.findAllByUserId(userId);

        return myChatUsers.stream().map(cu -> {
            Chat chat = cu.getChat();

            // 참여자 목록 (나 제외)
            List<ParticipantDto> participants = chatUserRepository.findAllByChatId(chat.getId())
                    .stream()
                    .map(ChatUser::getUser)
                    .filter(u -> !u.getId().equals(userId))
                    .map(u -> new ParticipantDto(u.getId(), u.getNickname(), u.getProfileImg()))
                    .toList();

            // 최신 메시지 조회
            ChatMessage lastMessageEntity = chatMessageRepository.findTopByChatIdOrderByCreatedAtDesc(chat.getId())
                    .orElse(null);

            ChatMessageDto lastMessageDto = null;
            Integer countMessages = null;
            if (lastMessageEntity != null) {
                lastMessageDto = new ChatMessageDto(
                        lastMessageEntity.getId(),
                        chat.getId(),
                        lastMessageEntity.getSender().getId(),
                        lastMessageEntity.getContent(),
                        lastMessageEntity.getRead(),
                        lastMessageEntity.getChatImg(),
                        lastMessageEntity.getCreatedAt(),
                        lastMessageEntity.getModifiedAt()
                );
                if (!lastMessageDto.getSenderId().equals(userId)){
                    countMessages = chatMessageRepository.countByChatIdAndSenderIdAndReadFalse(chat.getId(), lastMessageDto.getSenderId());
                }
            }




            return new ChatRoomDto(chat.getId(), participants, lastMessageDto, countMessages);
        }).toList();
    }

    @Transactional
    public void deleteChatRoom(Long chatId, Integer userId) {
        ChatUser chatUser = chatUserRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없거나 참여자가 아닙니다."));

        chatMessageRepository.deleteByChatId(chatId);
        chatUserRepository.deleteByChatId(chatId);
        chatRepository.deleteById(chatId);
    }

    public BlockUserDto blockUser(Integer userId, Integer blockId) {
        User blocked = userRepository.findById(blockId)
                .orElseThrow(()-> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "차단할 유저를 찾을 수 없습니다."
                ));

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User Not Found"
                ));

        BlockUser blockUser = new BlockUser();
        blockUser.setUser(user);
        blockUser.setBlocked(blocked);
        blockUserRepository.save(blockUser);

        return new BlockUserDto(blockUser);
    }



}
