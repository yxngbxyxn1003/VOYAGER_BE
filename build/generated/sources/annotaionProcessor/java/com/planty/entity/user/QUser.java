package com.planty.entity.user;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 1057964278L;

    public static final QUser user = new QUser("user");

    public final ListPath<BlockUser, QBlockUser> blocks = this.<BlockUser, QBlockUser>createList("blocks", BlockUser.class, QBlockUser.class, PathInits.DIRECT2);

    public final ListPath<com.planty.entity.board.Board, com.planty.entity.board.QBoard> boards = this.<com.planty.entity.board.Board, com.planty.entity.board.QBoard>createList("boards", com.planty.entity.board.Board.class, com.planty.entity.board.QBoard.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final ListPath<com.planty.entity.crop.Crop, com.planty.entity.crop.QCrop> crops = this.<com.planty.entity.crop.Crop, com.planty.entity.crop.QCrop>createList("crops", com.planty.entity.crop.Crop.class, com.planty.entity.crop.QCrop.class, PathInits.DIRECT2);

    public final ListPath<com.planty.entity.diary.Diary, com.planty.entity.diary.QDiary> diaries = this.<com.planty.entity.diary.Diary, com.planty.entity.diary.QDiary>createList("diaries", com.planty.entity.diary.Diary.class, com.planty.entity.diary.QDiary.class, PathInits.DIRECT2);

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final NumberPath<Integer> point = createNumber("point", Integer.class);

    public final StringPath profileImg = createString("profileImg");

    public final StringPath userId = createString("userId");

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

