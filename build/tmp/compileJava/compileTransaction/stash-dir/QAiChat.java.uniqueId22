package com.planty.entity.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAiChat is a Querydsl query type for AiChat
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAiChat extends EntityPathBase<AiChat> {

    private static final long serialVersionUID = 510620160L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAiChat aiChat = new QAiChat("aiChat");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<AiMessage, QAiMessage> messages = this.<AiMessage, QAiMessage>createList("messages", AiMessage.class, QAiMessage.class, PathInits.DIRECT2);

    public final com.planty.entity.user.QUser user;

    public QAiChat(String variable) {
        this(AiChat.class, forVariable(variable), INITS);
    }

    public QAiChat(Path<? extends AiChat> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAiChat(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAiChat(PathMetadata metadata, PathInits inits) {
        this(AiChat.class, metadata, inits);
    }

    public QAiChat(Class<? extends AiChat> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.planty.entity.user.QUser(forProperty("user")) : null;
    }

}

