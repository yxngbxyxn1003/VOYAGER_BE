package com.planty.entity.diary;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDiaryImage is a Querydsl query type for DiaryImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDiaryImage extends EntityPathBase<DiaryImage> {

    private static final long serialVersionUID = 536840379L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDiaryImage diaryImage = new QDiaryImage("diaryImage");

    public final DatePath<java.time.LocalDate> createdAt = createDate("createdAt", java.time.LocalDate.class);

    public final QDiary diary;

    public final StringPath diaryImg = createString("diaryImg");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final BooleanPath thumbnail = createBoolean("thumbnail");

    public QDiaryImage(String variable) {
        this(DiaryImage.class, forVariable(variable), INITS);
    }

    public QDiaryImage(Path<? extends DiaryImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDiaryImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDiaryImage(PathMetadata metadata, PathInits inits) {
        this(DiaryImage.class, metadata, inits);
    }

    public QDiaryImage(Class<? extends DiaryImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.diary = inits.isInitialized("diary") ? new QDiary(forProperty("diary"), inits.get("diary")) : null;
    }

}

