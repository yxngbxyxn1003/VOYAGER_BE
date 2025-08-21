package com.planty.entity.chat;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatUser is a Querydsl query type for ChatUser
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatUser extends EntityPathBase<ChatUser> {

    private static final long serialVersionUID = -1814505983L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatUser chatUser = new QChatUser("chatUser");

    public final QChat chat;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.planty.entity.user.QUser user;

    public QChatUser(String variable) {
        this(ChatUser.class, forVariable(variable), INITS);
    }

    public QChatUser(Path<? extends ChatUser> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatUser(PathMetadata metadata, PathInits inits) {
        this(ChatUser.class, metadata, inits);
    }

    public QChatUser(Class<? extends ChatUser> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chat = inits.isInitialized("chat") ? new QChat(forProperty("chat")) : null;
        this.user = inits.isInitialized("user") ? new com.planty.entity.user.QUser(forProperty("user")) : null;
    }

}

