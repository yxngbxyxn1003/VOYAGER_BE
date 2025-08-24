package com.planty.entity.diary;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDiary is a Querydsl query type for Diary
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDiary extends EntityPathBase<Diary> {

    private static final long serialVersionUID = 863273088L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDiary diary = new QDiary("diary");

    public final StringPath analysis = createString("analysis");

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final com.planty.entity.crop.QCrop crop;

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final ListPath<DiaryImage, QDiaryImage> images = this.<DiaryImage, QDiaryImage>createList("images", DiaryImage.class, QDiaryImage.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final StringPath title = createString("title");

    public final com.planty.entity.user.QUser user;

    public QDiary(String variable) {
        this(Diary.class, forVariable(variable), INITS);
    }

    public QDiary(Path<? extends Diary> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDiary(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDiary(PathMetadata metadata, PathInits inits) {
        this(Diary.class, metadata, inits);
    }

    public QDiary(Class<? extends Diary> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.crop = inits.isInitialized("crop") ? new com.planty.entity.crop.QCrop(forProperty("crop"), inits.get("crop")) : null;
        this.user = inits.isInitialized("user") ? new com.planty.entity.user.QUser(forProperty("user")) : null;
    }

}

