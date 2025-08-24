package com.planty.entity.chat;

import com.planty.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_users", uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id","user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
