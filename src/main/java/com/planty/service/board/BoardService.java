package com.planty.service.board;

import com.planty.dto.board.*;
import com.planty.entity.board.Board;
import com.planty.entity.board.BoardImage;
import com.planty.entity.crop.Crop;
import com.planty.entity.diary.Diary;
import com.planty.entity.diary.DiaryImage;
import com.planty.entity.user.User;
import com.planty.repository.board.BoardRepository;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.diary.DiaryRepository;
import com.planty.repository.user.UserRepository;
import com.planty.storage.StorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.planty.storage.ImageUrlMapper;

// 판매 게시판
@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;
    private final StorageService storageService;
    private final DiaryRepository diaryRepository;
    private final ImageUrlMapper imageUrlMapper;

    private final Integer MINUS_POINT = 200;

    // 판매 가능한 작물 목록 불러오기 (harvest=true)
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<BoardSellCropsResDto> getSellCrops(Integer userId) {
        List<Crop> crops = cropRepository.findByUser_IdAndHarvestTrueOrderByCreatedAtDesc(userId);

        return crops.stream()
                .map(BoardSellCropsResDto::of)
                .map(dto -> {
                    dto.setCropImg(imageUrlMapper.toPublic(dto.getCropImg()));
                    return dto;
                })
                .toList();
    }

    // 판매 게시글 작성
    public void saveBoard(Integer userId, BoardSaveFormDto dto) {
        User user = userRepository.getReferenceById(userId);

        // 보유 포인트가 적으면 실패
        if (user.getPoint() < MINUS_POINT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NOT_ENOUGH_POINT");
        }

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
        boardRepository.save(board);

        // 유저 포인트 차감
        user.setPoint(user.getPoint()-MINUS_POINT);

        // 유저 포인트 저장
        userRepository.save(user);
    }

    // 판매 게시글 상세 페이지 정보
    public BoardDetailResDto getBoardDetail(Integer id, Integer meId) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));

        // 판매자 정보
        SellerDto sellerDto = SellerDto.builder()
                .sellerId(board.getUser().getId())
                .sellerName(board.getUser().getNickname())
                .profileImg(imageUrlMapper.toPublic(board.getUser().getProfileImg()))
                .build();

        // 판매 게시글 이미지 처리
        List<String> images = Optional.ofNullable(board.getImages())
                .orElse(Collections.emptyList())
                .stream()
                .map(BoardImage::getBoardImg)
                .map(imageUrlMapper::toPublic)
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
        boolean isOwner = board.getUser().getId().equals(meId);

        // 프론트에 보내주는 Dto 반환
        return BoardDetailResDto.builder()
                .seller(sellerDto)
                .board(boardDetailDto)
                .isOwner(isOwner)
                .build();
    }

    // 판매 게시글 수정
    public void updateBoard(Integer boardId,
                                         Integer meId,
                                         BoardSaveFormDto dto,            // 새 제목/내용/가격/작물 + 새로 추가된 이미지 URL들
                                         List<String> keepImageUrls) {    // 유지할 기존 이미지 URL 목록 (null이면 유지 없음)

        // 1) 대상 게시글 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));

        // 2) 소유자 검증
        if (!board.getUser().getId().equals(meId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        // 3) 본문 필드 업데이트 (null 허용 정책은 필요에 맞게 조정)
        if (dto.getCropId() != null) {
            board.setCrop(cropRepository.getReferenceById(dto.getCropId()));
        }
        if (dto.getTitle() != null)   board.setTitle(dto.getTitle());
        if (dto.getContent() != null) board.setContent(dto.getContent());
        if (dto.getPrice() != null)   board.setPrice(dto.getPrice());

        // 4) 이미지 동기화
        // 현재 이미지들
        List<BoardImage> currentImages = board.getImages() != null ? board.getImages() : new ArrayList<>();

        // 유지할 목록 (null -> 빈 리스트)
        List<String> keep = keepImageUrls != null ? keepImageUrls : Collections.emptyList();

        // 새로 추가할 URL 목록 (컨트롤러에서 파일 업로드 후 전달된 것)
        List<String> newUrls = (dto.getImageUrls() != null) ? dto.getImageUrls() : Collections.emptyList();

        // (1) 삭제 대상 = 현재 - keep
        List<String> keepSet = new ArrayList<>(keep); // 순서 보존용
        List<BoardImage> toRemove = new ArrayList<>();
        for (BoardImage img : currentImages) {
            if (!keepSet.contains(img.getBoardImg())) {
                toRemove.add(img);
            }
        }

        for (BoardImage img : toRemove) {
            try {storageService.deleteByUrl(img.getBoardImg()); } catch (Exception ignore) {}
        }
        currentImages.removeAll(toRemove); // DB 고아 삭제(orphanRemoval=true)와 함께


        // (2) 새 이미지 추가 (중복 방지)
        // 이미 남아있는 URL 집합
        Set<String> remain = currentImages.stream().map(BoardImage::getBoardImg).collect(Collectors.toSet());
        Set<String> added = new HashSet<>();
        for (String url : newUrls) {
            if (url == null || url.isBlank()) continue;
            if (remain.contains(url) || added.contains(url)) continue;
            BoardImage bi = new BoardImage();
            bi.setBoard(board);
            bi.setBoardImg(url);
            bi.setThumbnail(false);
            currentImages.add(bi);
            added.add(url);
        }

        // (3) 썸네일 재지정
        // 규칙: 최종 이미지 목록의 첫 번째 이미지를 thumbnail=true로
        setThumbnailByOrder(currentImages, keepSet, newUrls);

        // 5) 저장
        boardRepository.save(board);
    }

    // 이미지 썸네일 지정
    private void setThumbnailByOrder(List<BoardImage> images,
                                     List<String> keepImageUrls,
                                     List<String> newUrls) {
        // 전체 타겟 순서
        List<String> ordered = new ArrayList<>();
        if (keepImageUrls != null) ordered.addAll(keepImageUrls);
        if (newUrls != null) ordered.addAll(newUrls);

        // 먼저 모두 false
        for (BoardImage img : images) {
            img.setThumbnail(false);
        }

        // ordered 기준으로 첫 번째로 매칭되는 이미지를 썸네일
        for (String first : ordered) {
            for (BoardImage img : images) {
                if (first != null && first.equals(img.getBoardImg())) {
                    img.setThumbnail(true);
                    return;
                }
            }
        }

        // ordered가 비었거나 매칭 실패 시, 남아있는 리스트 첫 번째를 썸네일
        if (!images.isEmpty()) {
            images.get(0).setThumbnail(true);
        }
    }

    // 판매 상태 변경
    public void updateSellStatus(Integer boardId,
                            Integer meId,
                            Boolean sellStatus) {    // 유지할 기존 이미지 URL 목록 (null이면 유지 없음)

        // 대상 게시글 조회 및 소유자 검증
        Board board = requireOwnBoard(boardId, meId);

        // 판매 상태 변경
        board.setSell(sellStatus);

        // 저장
        boardRepository.save(board);
    }

    // 판매 게시글 삭제
    public void deleteBoard(Integer boardId, Integer meId) {
        // 대상 게시글 조회 및 소유자 검증
        Board board = requireOwnBoard(boardId, meId);

        // 해당 게시글 삭제
        boardRepository.delete(board);
    }

    // 게시글 조회 및 소유자 검증
    private Board requireOwnBoard(Integer boardId,
                                  Integer meId){
        // 1) 대상 게시글 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));

        // 2) 소유자 검증
        if (!board.getUser().getId().equals(meId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        return board;
    }

    // 판매자 포인트 열람
    public PointResDto getPoint(Integer meId){
        // 판매자 조회
        User user = userRepository.findById(meId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));

        PointResDto pointResDto = PointResDto.builder()
                .id(meId)
                .point(user.getPoint())
                .build();

        // 판매자 정보 및 보유 포인트 반환
        return pointResDto;
    }

    // 전체 게시글 목록
    public List<BoardAllResDto> getAllBoards() {
        return boardRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(BoardAllResDto::of)
                .map(dto -> {
                    dto.setThumbnailImg(imageUrlMapper.toPublic(dto.getThumbnailImg()));
                    return dto;
                })
                .toList();
    }

    // 검색어로 판매 게시글 검색
    public List<BoardAllResDto> searchBoards(String keyword) {
        var list = (keyword == null || keyword.isBlank())
                ? boardRepository.findAllByOrderByCreatedAtDesc()
                : boardRepository.searchByKeyword("%" + keyword.trim() + "%");

        return list.stream()
                .map(BoardAllResDto::of)
                .map(dto -> {
                    dto.setThumbnailImg(imageUrlMapper.toPublic(dto.getThumbnailImg()));
                    return dto;
                })
                .toList();
    }


    // 판매 게시글의 재배 일지 목록
    public List<BoardDiaryResDto> getSellDiary(Integer boardId) {
        Integer cropId = boardRepository.findCropIdByBoardId(boardId);
        List<Diary> diaries = diaryRepository.findByCropIdOrderByCreatedAtDesc(cropId);

        return diaries.stream()
                .map(BoardDiaryResDto::of)
                .map(dto -> { // 🔴 썸네일 변환
                    dto.setThumbnailImg(imageUrlMapper.toPublic(dto.getThumbnailImg()));
                    return dto;
                })
                .toList();
    }


    // 판매 게시글의 재배 일지 상세 페이지
    public BoardDiaryDetailResDto getSellDiaryDetail(Integer diaryId, Integer meId) {
        // 대상 재배 일지 조회
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));

        // 소유자 여부
        boolean isOwner = diary.getUser().getId().equals(meId);

        // 판매 게시글 이미지 처리
        List<String> images = Optional.ofNullable(diary.getImages())
                .orElse(Collections.emptyList())
                .stream()
                .map(DiaryImage::getDiaryImg)
                .map(imageUrlMapper::toPublic)
                .toList();

        // 날짜만 추출
        String time = diary.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd."));

        // 프론트에 보내주는 Dto 반환
        return BoardDiaryDetailResDto.builder()
                .diaryId(diary.getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .images(images)
                .time(time)
                .isOwner(isOwner)
                .build();
    }
}
