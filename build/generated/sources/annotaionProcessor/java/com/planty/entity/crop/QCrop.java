package com.planty.entity.crop;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCrop is a Querydsl query type for Crop
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCrop extends EntityPathBase<Crop> {

    private static final long serialVersionUID = 2011870230L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCrop crop = new QCrop("crop");

    public final EnumPath<AnalysisStatus> analysisStatus = createEnum("analysisStatus", AnalysisStatus.class);

    public final ListPath<CropCategory, QCropCategory> categories = this.<CropCategory, QCropCategory>createList("categories", CropCategory.class, QCropCategory.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath cropImg = createString("cropImg");

    public final DatePath<java.time.LocalDate> endAt = createDate("endAt", java.time.LocalDate.class);

    public final StringPath environment = createString("environment");

    public final BooleanPath harvest = createBoolean("harvest");

    public final StringPath height = createString("height");

    public final StringPath howTo = createString("howTo");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final BooleanPath isRegistered = createBoolean("isRegistered");

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final StringPath name = createString("name");

    public final DatePath<java.time.LocalDate> startAt = createDate("startAt", java.time.LocalDate.class);

    public final StringPath temperature = createString("temperature");

    public final com.planty.entity.user.QUser user;

    public QCrop(String variable) {
        this(Crop.class, forVariable(variable), INITS);
    }

    public QCrop(Path<? extends Crop> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCrop(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCrop(PathMetadata metadata, PathInits inits) {
        this(Crop.class, metadata, inits);
    }

    public QCrop(Class<? extends Crop> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.planty.entity.user.QUser(forProperty("user")) : null;
    }

}

