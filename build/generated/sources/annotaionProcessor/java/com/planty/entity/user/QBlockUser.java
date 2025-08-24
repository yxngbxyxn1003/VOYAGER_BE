package com.planty.entity.user;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBlockUser is a Querydsl query type for BlockUser
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBlockUser extends EntityPathBase<BlockUser> {

    private static final long serialVersionUID = -780778579L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBlockUser blockUser = new QBlockUser("blockUser");

    public final QUser blocked;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final QUser user;

    public QBlockUser(String variable) {
        this(BlockUser.class, forVariable(variable), INITS);
    }

    public QBlockUser(Path<? extends BlockUser> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBlockUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBlockUser(PathMetadata metadata, PathInits inits) {
        this(BlockUser.class, metadata, inits);
    }

    public QBlockUser(Class<? extends BlockUser> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.blocked = inits.isInitialized("blocked") ? new QUser(forProperty("blocked")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}

