package com.planty.entity.crop;

public enum AnalysisStatus {
    PENDING,     // 분석 대기중
    ANALYZING,   // 분석 중 (등록중)
    COMPLETED,   // 분석 완료
    FAILED       // 분석 실패
}
