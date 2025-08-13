package com.planty.service.board;

import com.planty.dto.board.BoardSaveFormDto;
import com.planty.dto.board.BoardSellCropsDto;
import com.planty.entity.board.Board;
import com.planty.entity.board.BoardImage;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.repository.board.BoardRepository;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


// 판매 게시판
@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;

    // 판매 가능한 작물 목록 불러오기 (harvest=true)
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<BoardSellCropsDto> getSellCrops(Integer userId) {
        // 수확 완료 작물 목록을 최신순으로 조회
        List<Crop> crops = cropRepository.findByUser_IdAndHarvestTrueOrderByCreatedAtDesc(userId);

        // Crop 엔티티를 BoardSellCropsDto로 변환, 리스트로 반환
        return crops.stream()
                .map(BoardSellCropsDto::of)
                .toList();
    }

    // 판매 게시글 작성
    public Integer saveBoard(Integer userId, BoardSaveFormDto dto) {
        User user = userRepository.getReferenceById(userId);
        Crop crop = cropRepository.getReferenceById(dto.getCropId());

        // 판매 게시글 생성 및 데이터 삽입
        Board board = new Board();
        board.setUser(user);
        board.setCrop(crop);
        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());
        board.setPrice(dto.getPrice());
        board.setSell(false);

        // 판매 게시글 이미지 삽입
        List<BoardImage> imgs = new ArrayList<>();
        for (int i = 0; i < dto.getImageUrls().size(); i++) {
            BoardImage bi = new BoardImage();
            bi.setBoard(board);
            bi.setBoardImg(dto.getImageUrls().get(i));
            bi.setThumbnail(i == 0);
            imgs.add(bi);
        }
        board.setImages(imgs);

        // 판매 게시글 저장
        return boardRepository.save(board).getId();
    }

}
