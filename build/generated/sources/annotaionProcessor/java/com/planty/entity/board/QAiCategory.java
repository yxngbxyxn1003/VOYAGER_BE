package com.planty.entity.board;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAiCategory is a Querydsl query type for AiCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAiCategory extends EntityPathBase<AiCategory> {

    private static final long serialVersionUID = 1122240422L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAiCategory aiCategory = new QAiCategory("aiCategory");

    public final QAiMessage aiMessage;

    public final StringPath categoryName = createString("categoryName");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public QAiCategory(String variable) {
        this(AiCategory.class, forVariable(variable), INITS);
    }

    public QAiCategory(Path<? extends AiCategory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAiCategory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAiCategory(PathMetadata metadata, PathInits inits) {
        this(AiCategory.class, metadata, inits);
    }

    public QAiCategory(Class<? extends AiCategory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.aiMessage = inits.isInitialized("aiMessage") ? new QAiMessage(forProperty("aiMessage"), inits.get("aiMessage")) : null;
    }

}

