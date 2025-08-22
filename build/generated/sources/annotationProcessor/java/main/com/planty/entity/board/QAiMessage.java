package com.planty.entity.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAiMessage is a Querydsl query type for AiMessage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAiMessage extends EntityPathBase<AiMessage> {

    private static final long serialVersionUID = -673071009L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAiMessage aiMessage = new QAiMessage("aiMessage");

    public final QAiChat aiChat;

    public final StringPath aiImage = createString("aiImage");

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final StringPath sender = createString("sender");

    public QAiMessage(String variable) {
        this(AiMessage.class, forVariable(variable), INITS);
    }

    public QAiMessage(Path<? extends AiMessage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAiMessage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAiMessage(PathMetadata metadata, PathInits inits) {
        this(AiMessage.class, metadata, inits);
    }

    public QAiMessage(Class<? extends AiMessage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.aiChat = inits.isInitialized("aiChat") ? new QAiChat(forProperty("aiChat"), inits.get("aiChat")) : null;
    }

}

