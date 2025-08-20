package com.planty.entity.crop;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCropCategory is a Querydsl query type for CropCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCropCategory extends EntityPathBase<CropCategory> {

    private static final long serialVersionUID = -793419980L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCropCategory cropCategory = new QCropCategory("cropCategory");

    public final StringPath categoryName = createString("categoryName");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final QCrop crop;

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public QCropCategory(String variable) {
        this(CropCategory.class, forVariable(variable), INITS);
    }

    public QCropCategory(Path<? extends CropCategory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCropCategory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCropCategory(PathMetadata metadata, PathInits inits) {
        this(CropCategory.class, metadata, inits);
    }

    public QCropCategory(Class<? extends CropCategory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.crop = inits.isInitialized("crop") ? new QCrop(forProperty("crop"), inits.get("crop")) : null;
    }

}

