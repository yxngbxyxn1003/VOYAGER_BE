package com.planty.service.board;

import com.planty.dto.board.BoardSellCropsDto;
import com.planty.entity.crop.Crop;
import com.planty.repository.board.BoardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


// 판매 게시판
@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    // 판매 가능한 작물 목록 불러오기 (harvest=true)
    @Transactional(Transactional.TxType.SUPPORTS) // 읽기 전용
    public List<BoardSellCropsDto> getSellCrops(Integer userId) {
        // 수확 완료 작물 목록을 최신순으로 조회
        List<Crop> crops = boardRepository.findByUserIdAndHarvestTrueOrderByCreatedAtDesc(userId);

        // Crop 엔티티를 BoardSellCropDto로 변환, 리스트로 반환
        return crops.stream()
                .map(BoardSellCropsDto::of)
                .toList();
    }
}
