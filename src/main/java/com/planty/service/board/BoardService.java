package com.planty.service.board;

import com.planty.config.CustomUserDetails;
import com.planty.dto.board.*;
import com.planty.entity.board.Board;
import com.planty.entity.board.BoardImage;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.repository.board.BoardRepository;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


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

    // 판매 게시글 상세 페이지 정보
    public BoardDetailResDto getBoardDetail(Integer id, CustomUserDetails me) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));

        // 판매자 정보
        SellerDto sellerDto = SellerDto.builder()
                .sellerId(board.getUser().getId())
                .sellerName(board.getUser().getNickname())
                .profileImg(board.getUser().getProfileImg())
                .build();

        // 판매 게시글 이미지 처리
        List<String> images = Optional.ofNullable(board.getImages())
                .orElse(Collections.emptyList())
                .stream()
                .map(BoardImage::getBoardImg)
                .toList();

        // 판매 게시글 정보
        BoardDetailDto boardDetailDto = BoardDetailDto.builder()
                .boardId(board.getId())
                .cropId(board.getCrop().getId())
                .title(board.getTitle())
                .content(board.getContent())
                .price(board.getPrice())
                .sell(board.getSell())
                .images(images)
                .build();

        // 소유자 여부
        boolean isOwner = me != null && board.getUser().getId().equals(me.getId());

        // 프론트에 보내주는 Dto 반환
        return BoardDetailResDto.builder()
                .seller(sellerDto)
                .board(boardDetailDto)
                .isOwner(isOwner)
                .build();
    }
}
